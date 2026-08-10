package com.jclinical.integrations.infra.adapters.out.persistence.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Converter
@Component("integrationsAesCryptoConverter")
@Slf4j
public class AesCryptoConverter implements AttributeConverter<String, String> {

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String PREFIX = "ENC:";

    private final SecretKeySpec secretKeySpec;
    private final byte[] ivBytes;

    public AesCryptoConverter(@Value("${medicloud.security.encryption-key:DefaultSecretEncryptionKey32Chars!}") String secretKey) {
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
            if (finalKey != secretKey) {
                MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
                keyBytes = sha256.digest(rawBytes);
            } else {
                System.arraycopy(rawBytes, 0, keyBytes, 0, Math.min(rawBytes.length, 32));
            }

            this.secretKeySpec = new SecretKeySpec(keyBytes, "AES");

            MessageDigest md5 = MessageDigest.getInstance("MD5");
            this.ivBytes = md5.digest(keyBytes);
        } catch (Exception e) {
            log.error("Error inicializando AesCryptoConverter", e);
            throw new IllegalStateException("No se pudo inicializar el conversor criptográfico", e);
        }
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return attribute;
        }
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, new IvParameterSpec(ivBytes));
            byte[] encrypted = cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8));
            return PREFIX + Base64.getEncoder().encodeToString(encrypted);
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
        if (!dbData.startsWith(PREFIX)) {
            log.debug("Detección de texto plano en base de datos (sin prefijo '{}')", PREFIX);
            return dbData;
        }
        try {
            String cipherText = dbData.substring(PREFIX.length());
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new IvParameterSpec(ivBytes));
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(cipherText));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("Fallo al descifrar atributo conteniendo prefijo '{}'. Se retorna el valor original.", PREFIX, e);
            return dbData;
        }
    }
}
