package com.jclinical.core.security.crypto;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicLong;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Motor de cifrado de campos de base de datos, unificado para todos los módulos.
 *
 * <p>Antes vivía duplicado byte a byte en {@code records-infra} e
 * {@code integrations-infra}; cualquier divergencia dejaba datos ilegibles en
 * silencio. Ahora el algoritmo, el formato y la derivación de llave están aquí y
 * los conversores JPA de cada módulo son adaptadores finos.</p>
 *
 * <p>Formatos:</p>
 * <ul>
 *   <li>{@code ENC_GCM_V2:} — escritura actual. AES-256/GCM con llave derivada por
 *       PBKDF2-HMAC-SHA256 ({@value #PBKDF2_ITERATIONS} iteraciones).</li>
 *   <li>{@code ENC_GCM:} — lectura de datos v1 (llave = SHA-256 del material). Se
 *       sigue leyendo hasta que se corra el re-cifrado masivo.</li>
 *   <li>{@code ENC:} — lectura legacy AES/CBC con IV = MD5(llave). Determinista y
 *       sin autenticación; pendiente de retiro tras la migración.</li>
 * </ul>
 *
 * <p>Un fallo de descifrado de un valor con prefijo lanza {@link FieldCryptoException}
 * en vez de devolver el ciphertext. Un valor sin prefijo se devuelve tal cual pero
 * incrementa un contador observable ({@link #getPlaintextReads()}).</p>
 */
public final class FieldCipher {

    public static final String PREFIX_V2 = "ENC_GCM_V2:";
    public static final String PREFIX_V1 = "ENC_GCM:";
    public static final String PREFIX_LEGACY = "ENC:";

    private static final String GCM_ALGORITHM = "AES/GCM/NoPadding";
    private static final String LEGACY_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final int GCM_IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    private static final int PBKDF2_ITERATIONS = 210_000;
    private static final int DERIVED_KEY_BITS = 256;
    private static final byte[] V2_KDF_SALT =
            "medicloud/field-crypto/v2".getBytes(StandardCharsets.UTF_8);

    private static final String HW_MACHINE_ID_FILE = "/run/secrets/machine_id";
    private static final String HW_PRODUCT_UUID_FILE = "/run/secrets/product_uuid";
    private static final String KEY_FILE_NAME = ".field-key";
    /** Ubicación anterior; se lee para no perder datos v1, nunca se escribe. */
    private static final String LEGACY_KEY_FILE = "/app/uploads/.secure_key";

    private final SecretKeySpec v2Key;
    private final SecretKeySpec v1Key;
    private final SecureRandom secureRandom = new SecureRandom();
    private final AtomicLong plaintextReads = new AtomicLong();

    FieldCipher(SecretKeySpec v2Key, SecretKeySpec v1Key) {
        this.v2Key = v2Key;
        this.v1Key = v1Key;
    }

    /**
     * Construye el cifrador resolviendo el material de llave del entorno.
     *
     * @param passphrase   valor de {@code medicloud.security.encryption-key}
     * @param keyStoreDir  directorio donde persiste {@code .field-key} cuando no hay
     *                     secretos de hardware montados; {@code null} o inexistente
     *                     ⇒ no se liga a archivo (dev/test).
     */
    public static FieldCipher fromConfiguration(String passphrase, Path keyStoreDir) {
        if (passphrase == null || passphrase.isBlank()) {
            throw new FieldCryptoException(
                    "medicloud.security.encryption-key no está definida; la aplicación no debe arrancar sin llave.");
        }
        try {
            String bindingMaterial = resolveBindingMaterial(passphrase, keyStoreDir);
            SecretKeySpec v2 = deriveV2Key(bindingMaterial);
            SecretKeySpec v1 = deriveV1Key(passphrase, bindingMaterial);
            return new FieldCipher(v2, v1);
        } catch (FieldCryptoException e) {
            throw e;
        } catch (Exception e) {
            throw new FieldCryptoException("No se pudo inicializar el cifrador de campos.", e);
        }
    }

    private static String resolveBindingMaterial(String passphrase, Path keyStoreDir)
            throws IOException, NoSuchAlgorithmException {
        Path machineId = Path.of(HW_MACHINE_ID_FILE);
        Path productUuid = Path.of(HW_PRODUCT_UUID_FILE);
        if (Files.exists(machineId) && Files.exists(productUuid)) {
            String m = Files.readString(machineId).trim();
            String p = Files.readString(productUuid).trim();
            if (!m.isEmpty() && !p.isEmpty()) {
                return m + ":" + p + ":" + passphrase;
            }
        }

        if (keyStoreDir != null && Files.isDirectory(keyStoreDir)) {
            Path keyFile = keyStoreDir.resolve(KEY_FILE_NAME);
            if (!Files.exists(keyFile)) {
                Path legacy = Path.of(LEGACY_KEY_FILE);
                if (Files.exists(legacy)) {
                    // Migración transparente: mover el material, no regenerarlo.
                    Files.writeString(keyFile, Files.readString(legacy).trim(), StandardCharsets.UTF_8);
                } else {
                    byte[] random = new byte[32];
                    SecureRandom.getInstanceStrong().nextBytes(random);
                    Files.writeString(keyFile, Base64.getEncoder().encodeToString(random), StandardCharsets.UTF_8);
                }
                restrictPermissions(keyFile);
            }
            return Files.readString(keyFile).trim() + ":" + passphrase;
        }

        // Sin hardware ni almacén de llave (entornos de desarrollo y pruebas).
        return passphrase;
    }

    private static void restrictPermissions(Path keyFile) {
        try {
            Files.setPosixFilePermissions(keyFile,
                    java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
        } catch (UnsupportedOperationException | IOException ignored) {
            // Sistemas sin POSIX (Windows dev): el ACL por defecto es suficiente.
        }
    }

    private static SecretKeySpec deriveV2Key(String bindingMaterial) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        PBEKeySpec spec = new PBEKeySpec(
                bindingMaterial.toCharArray(), V2_KDF_SALT, PBKDF2_ITERATIONS, DERIVED_KEY_BITS);
        try {
            SecretKey key = factory.generateSecret(spec);
            return new SecretKeySpec(key.getEncoded(), "AES");
        } finally {
            spec.clearPassword();
        }
    }

    @SuppressWarnings("java:S4790") // SHA-256 aquí no es hashing de credenciales, es compat de lectura v1
    private static SecretKeySpec deriveV1Key(String passphrase, String bindingMaterial) throws NoSuchAlgorithmException {
        byte[] keyBytes = new byte[32];
        if (!bindingMaterial.equals(passphrase)) {
            keyBytes = MessageDigest.getInstance("SHA-256")
                    .digest(bindingMaterial.getBytes(StandardCharsets.UTF_8));
        } else {
            byte[] raw = passphrase.getBytes(StandardCharsets.UTF_8);
            System.arraycopy(raw, 0, keyBytes, 0, Math.min(raw.length, 32));
        }
        return new SecretKeySpec(keyBytes, "AES");
    }

    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(GCM_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, v2Key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] ct = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(ct, 0, payload, iv.length, ct.length);
            return PREFIX_V2 + Base64.getEncoder().encodeToString(payload);
        } catch (Exception e) {
            throw new FieldCryptoException("Error al cifrar atributo.", e);
        }
    }

    public String decrypt(String stored) {
        if (stored == null || stored.isEmpty()) {
            return stored;
        }
        if (stored.startsWith(PREFIX_V2)) {
            return gcmDecrypt(stored.substring(PREFIX_V2.length()), v2Key, PREFIX_V2);
        }
        if (stored.startsWith(PREFIX_V1)) {
            return gcmDecrypt(stored.substring(PREFIX_V1.length()), v1Key, PREFIX_V1);
        }
        if (stored.startsWith(PREFIX_LEGACY)) {
            return legacyDecrypt(stored.substring(PREFIX_LEGACY.length()));
        }
        plaintextReads.incrementAndGet();
        return stored;
    }

    private String gcmDecrypt(String base64Payload, SecretKeySpec key, String prefix) {
        try {
            byte[] payload = Base64.getDecoder().decode(base64Payload);
            if (payload.length <= GCM_IV_LENGTH_BYTES) {
                throw new IllegalArgumentException("payload cifrado incompleto");
            }
            byte[] iv = Arrays.copyOfRange(payload, 0, GCM_IV_LENGTH_BYTES);
            byte[] ct = Arrays.copyOfRange(payload, GCM_IV_LENGTH_BYTES, payload.length);
            Cipher cipher = Cipher.getInstance(GCM_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(ct), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new FieldCryptoException(
                    "Fallo al descifrar un valor con prefijo '" + prefix + "'.", e);
        }
    }

    @SuppressWarnings({"java:S4790", "java:S5542"}) // formato legacy, solo lectura
    private String legacyDecrypt(String base64Payload) {
        try {
            byte[] iv = MessageDigest.getInstance("MD5").digest(v1Key.getEncoded());
            Cipher cipher = Cipher.getInstance(LEGACY_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, v1Key, new IvParameterSpec(iv));
            return new String(cipher.doFinal(Base64.getDecoder().decode(base64Payload)), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new FieldCryptoException(
                    "Fallo al descifrar un valor con prefijo '" + PREFIX_LEGACY + "'.", e);
        }
    }

    /** Nº de lecturas de valores sin prefijo de cifrado desde el arranque (para métrica/gauge). */
    public long getPlaintextReads() {
        return plaintextReads.get();
    }
}
