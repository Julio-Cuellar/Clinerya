package com.jclinical.records.infra.adapters.out.persistence.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Converter
@Component
@Slf4j
@SuppressWarnings({"java:S4790", "java:S5542"})
public class AesCryptoConverter implements AttributeConverter<String, String> {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final String LEGACY_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String PREFIX = "ENC_GCM:";
    private static final String LEGACY_PREFIX = "ENC:";
    private static final int GCM_IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    private final SecretKeySpec secretKeySpec;
    private final SecureRandom secureRandom = new SecureRandom();

    public AesCryptoConverter(@Value("${medicloud.security.encryption-key:***ENCRYPTION_KEY_DEFAULT_REMOVED***}") String secretKey) {
        try {
            byte[] keyBytes = new byte[32];
            String finalKey = secretKey;

            java.io.File machineIdFile = new java.io.File("/run/secrets/machine_id");
            java.io.File productUuidFile = new java.io.File("/run/secrets/product_uuid");

            if (machineIdFile.exists() && productUuidFile.exists()) {
                String machineId = java.nio.file.Files.readString(machineIdFile.toPath()).trim();
                String productUuid = java.nio.file.Files.readString(productUuidFile.toPath()).trim();
                if (!machineId.isEmpty() && !productUuid.isEmpty()) {
                    finalKey = machineId + ":" + productUuid + ":" + secretKey;
                }
            } else {
                java.io.File uploadsDir = new java.io.File("/app/uploads");
                if (uploadsDir.exists()) {
                    java.io.File secureKeyFile = new java.io.File(uploadsDir, ".secure_key");
                    if (!secureKeyFile.exists()) {
                        byte[] randomBytes = new byte[32];
                        java.security.SecureRandom.getInstanceStrong().nextBytes(randomBytes);
                        String generatedKey = Base64.getEncoder().encodeToString(randomBytes);
                        java.nio.file.Files.writeString(secureKeyFile.toPath(), generatedKey, StandardCharsets.UTF_8);
                    }
                    String savedKey = java.nio.file.Files.readString(secureKeyFile.toPath()).trim();
                    finalKey = savedKey + ":" + secretKey;
                }
            }

            byte[] rawBytes = finalKey.getBytes(StandardCharsets.UTF_8);
            if (!finalKey.equals(secretKey)) {
                MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
                keyBytes = sha256.digest(rawBytes);
            } else {
                System.arraycopy(rawBytes, 0, keyBytes, 0, Math.min(rawBytes.length, 32));
            }

            this.secretKeySpec = new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            log.error("Error inicializando AesCryptoConverter", e);
            throw new IllegalStateException("No se pudo inicializar el conversor criptografico", e);
        }
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return attribute;
        }
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            byte[] ivBytes = new byte[GCM_IV_LENGTH_BYTES];
            secureRandom.nextBytes(ivBytes);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, ivBytes));
            byte[] encrypted = cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[ivBytes.length + encrypted.length];
            System.arraycopy(ivBytes, 0, payload, 0, ivBytes.length);
            System.arraycopy(encrypted, 0, payload, ivBytes.length, encrypted.length);
            return PREFIX + Base64.getEncoder().encodeToString(payload);
        } catch (Exception e) {
            log.error("Error al cifrar atributo", e);
            throw new RuntimeException("Error al cifrar atributo", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return dbData;
        }
        if (dbData.startsWith(LEGACY_PREFIX) && !dbData.startsWith(PREFIX)) {
            return decryptLegacyValue(dbData);
        }
        if (!dbData.startsWith(PREFIX)) {
            log.debug("Deteccion de texto plano en base de datos (sin prefijo '{}')", PREFIX);
            return dbData;
        }
        try {
            byte[] payload = Base64.getDecoder().decode(dbData.substring(PREFIX.length()));
            if (payload.length <= GCM_IV_LENGTH_BYTES) {
                throw new IllegalArgumentException("Payload cifrado incompleto");
            }
            byte[] ivBytes = Arrays.copyOfRange(payload, 0, GCM_IV_LENGTH_BYTES);
            byte[] encrypted = Arrays.copyOfRange(payload, GCM_IV_LENGTH_BYTES, payload.length);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, ivBytes));
            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("Fallo al descifrar atributo conteniendo prefijo '{}'. Se retorna el valor original.", PREFIX, e);
            return dbData;
        }
    }

    private String decryptLegacyValue(String dbData) {
        try {
            byte[] legacyIvBytes = MessageDigest.getInstance("MD5").digest(secretKeySpec.getEncoded());
            Cipher cipher = Cipher.getInstance(LEGACY_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new IvParameterSpec(legacyIvBytes));
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(dbData.substring(LEGACY_PREFIX.length())));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("Fallo al descifrar atributo legacy conteniendo prefijo '{}'. Se retorna el valor original.", LEGACY_PREFIX, e);
            return dbData;
        }
    }
}
