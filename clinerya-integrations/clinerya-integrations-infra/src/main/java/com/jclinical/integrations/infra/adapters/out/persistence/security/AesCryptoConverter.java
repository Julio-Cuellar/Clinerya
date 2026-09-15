package com.jclinical.integrations.infra.adapters.out.persistence.security;

import com.jclinical.core.security.crypto.FieldCipher;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

/**
 * Adaptador JPA fino sobre {@link FieldCipher}. Toda la criptografía vive en
 * {@code clinerya-core}; aquí solo se resuelve la configuración de Spring y se
 * delega, para que esta copia y la de {@code records-infra} no puedan divergir.
 */
@Converter
@Component("integrationsAesCryptoConverter")
public class AesCryptoConverter implements AttributeConverter<String, String> {

    private final FieldCipher cipher;

    public AesCryptoConverter(
            @Value("${medicloud.security.encryption-key}") String encryptionKey,
            @Value("${medicloud.security.key-store-dir:/app/secrets}") String keyStoreDir) {
        this.cipher = FieldCipher.fromConfiguration(
                encryptionKey, keyStoreDir == null || keyStoreDir.isBlank() ? null : Path.of(keyStoreDir));
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return cipher.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return cipher.decrypt(dbData);
    }
}
