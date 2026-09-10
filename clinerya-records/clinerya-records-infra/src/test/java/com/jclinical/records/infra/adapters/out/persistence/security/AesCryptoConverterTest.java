package com.jclinical.records.infra.adapters.out.persistence.security;

import com.jclinical.core.security.crypto.FieldCryptoException;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AesCryptoConverterTest {

    private final String testKey = "MySuperSecretKeyForTestingAES256!";
    private final AesCryptoConverter converter = new AesCryptoConverter(testKey, null);

    @Test
    void shouldEncryptWithV2FormatAndDecrypt() {
        String plainText = "{\"sintomas\":\"dolor de cabeza\",\"edad\":28}";

        String encrypted = converter.convertToDatabaseColumn(plainText);
        assertThat(encrypted).startsWith("ENC_GCM_V2:");
        assertThat(encrypted).isNotEqualTo(plainText);

        assertThat(converter.convertToEntityAttribute(encrypted)).isEqualTo(plainText);
    }

    @Test
    void shouldUseRandomIvForEachEncryption() {
        String plainText = "{\"sintomas\":\"dolor de cabeza\",\"edad\":28}";

        String first = converter.convertToDatabaseColumn(plainText);
        String second = converter.convertToDatabaseColumn(plainText);

        assertThat(first).isNotEqualTo(second);
        assertThat(converter.convertToEntityAttribute(first)).isEqualTo(plainText);
        assertThat(converter.convertToEntityAttribute(second)).isEqualTo(plainText);
    }

    @Test
    void shouldStillReadV1GcmValues() throws Exception {
        String plainText = "nota cifrada con el formato v1";

        String v1 = encryptV1GcmValue(plainText);

        assertThat(v1).startsWith("ENC_GCM:");
        assertThat(converter.convertToEntityAttribute(v1)).isEqualTo(plainText);
    }

    @Test
    void shouldStillReadLegacyCbcValues() throws Exception {
        String plainText = "Texto cifrado con formato anterior";

        String legacy = encryptLegacyValue(plainText);

        assertThat(legacy).startsWith("ENC:");
        assertThat(converter.convertToEntityAttribute(legacy)).isEqualTo(plainText);
    }

    @Test
    void shouldThrowInsteadOfReturningCiphertextWhenDecryptFails() {
        String tampered = "ENC_GCM_V2:" + Base64.getEncoder().encodeToString("no soy un payload valido".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> converter.convertToEntityAttribute(tampered))
                .isInstanceOf(FieldCryptoException.class);
    }

    @Test
    void shouldReturnPrefixlessValueAsPlaintextButCountIt() {
        String plainText = "Texto antiguo en texto plano";

        assertThat(converter.convertToEntityAttribute(plainText)).isEqualTo(plainText);
        assertThat(converter.cipher().getPlaintextReads()).isPositive();
    }

    @Test
    void shouldHandleNullOrEmpty() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToDatabaseColumn("")).isEmpty();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
        assertThat(converter.convertToEntityAttribute("")).isEmpty();
    }

    private SecretKeySpec legacyKey() {
        byte[] keyBytes = new byte[32];
        byte[] raw = testKey.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(raw, 0, keyBytes, 0, Math.min(raw.length, 32));
        return new SecretKeySpec(keyBytes, "AES");
    }

    private String encryptV1GcmValue(String plainText) throws Exception {
        SecretKeySpec key = legacyKey();
        byte[] iv = new byte[12];
        new java.security.SecureRandom().nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new javax.crypto.spec.GCMParameterSpec(128, iv));
        byte[] ct = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        byte[] payload = new byte[iv.length + ct.length];
        System.arraycopy(iv, 0, payload, 0, iv.length);
        System.arraycopy(ct, 0, payload, iv.length, ct.length);
        return "ENC_GCM:" + Base64.getEncoder().encodeToString(payload);
    }

    @SuppressWarnings({"java:S4790", "java:S5542"})
    private String encryptLegacyValue(String plainText) throws Exception {
        SecretKeySpec key = legacyKey();
        byte[] iv = MessageDigest.getInstance("MD5").digest(key.getEncoded());
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
        return "ENC:" + Base64.getEncoder().encodeToString(cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8)));
    }
}
