package com.jclinical.records.infra.adapters.out.persistence.security;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AesCryptoConverterTest {

    private final String testKey = "MySuperSecretKeyForTestingAES256!";
    private final AesCryptoConverter converter = new AesCryptoConverter(testKey);

    @Test
    void shouldEncryptAndDecryptSuccessfully() {
        String plainText = "{\"sintomas\":\"dolor de cabeza\",\"edad\":28}";
        
        String encrypted = converter.convertToDatabaseColumn(plainText);
        assertThat(encrypted).startsWith("ENC:");
        assertThat(encrypted).isNotEqualTo(plainText);

        String decrypted = converter.convertToEntityAttribute(encrypted);
        assertThat(decrypted).isEqualTo(plainText);
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
}
