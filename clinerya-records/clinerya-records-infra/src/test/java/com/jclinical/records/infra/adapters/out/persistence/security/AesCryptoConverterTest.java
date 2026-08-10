package com.jclinical.records.infra.adapters.out.persistence.security;

import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class AesCryptoConverterTest {

    private final String testKey = "MySuperSecretKeyForTestingAES256!";
    private final AesCryptoConverter converter = new AesCryptoConverter(testKey);

    @Test
    void shouldEncryptAndDecryptSuccessfully() {
        String plainText = "{\"sintomas\":\"dolor de cabeza\",\"edad\":28}";

        String encrypted = converter.convertToDatabaseColumn(plainText);
        assertThat(encrypted).startsWith("ENC_GCM:");
        assertThat(encrypted).isNotEqualTo(plainText);

        String decrypted = converter.convertToEntityAttribute(encrypted);
        assertThat(decrypted).isEqualTo(plainText);
    }

    @Test
    void shouldUseRandomIvForEachEncryption() {
        String plainText = "{\"sintomas\":\"dolor de cabeza\",\"edad\":28}";

        String firstEncrypted = converter.convertToDatabaseColumn(plainText);
        String secondEncrypted = converter.convertToDatabaseColumn(plainText);

        assertThat(firstEncrypted).isNotEqualTo(secondEncrypted);
        assertThat(converter.convertToEntityAttribute(firstEncrypted)).isEqualTo(plainText);
        assertThat(converter.convertToEntityAttribute(secondEncrypted)).isEqualTo(plainText);
    }

    @Test
    void shouldDecryptLegacyCbcValue() throws Exception {
        String plainText = "Texto cifrado con formato anterior";

        String legacyEncrypted = encryptLegacyValue(plainText);

        assertThat(legacyEncrypted).startsWith("ENC:");
        assertThat(converter.convertToEntityAttribute(legacyEncrypted)).isEqualTo(plainText);
    }

    @Test
    void shouldReturnOriginalTextWhenNoEncryptionPrefix() {
        String plainText = "Texto antiguo en texto plano";

        String result = converter.convertToEntityAttribute(plainText);
        assertThat(result).isEqualTo(plainText);
    }

    @Test
    void shouldHandleNullOrEmpty() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToDatabaseColumn("")).isEmpty();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
        assertThat(converter.convertToEntityAttribute("")).isEmpty();
    }

    @SuppressWarnings({"java:S4790", "java:S5542"})
    private String encryptLegacyValue(String plainText) throws Exception {
        byte[] keyBytes = new byte[32];
        byte[] rawBytes = testKey.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(rawBytes, 0, keyBytes, 0, Math.min(rawBytes.length, 32));
        SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "AES");
        byte[] ivBytes = MessageDigest.getInstance("MD5").digest(keyBytes);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, new IvParameterSpec(ivBytes));
        return "ENC:" + Base64.getEncoder().encodeToString(cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8)));
    }
}
