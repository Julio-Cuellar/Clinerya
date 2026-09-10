package com.jclinical.core.security.crypto;

/**
 * Se lanza cuando un valor que declara estar cifrado (prefijo {@code ENC_GCM_V2:},
 * {@code ENC_GCM:} o {@code ENC:}) no puede descifrarse. Antes el conversor hacía
 * {@code log.warn} y devolvía el ciphertext tal cual, lo que dejaba pasar datos
 * corruptos o manipulados como si fueran texto claro.
 */
public class FieldCryptoException extends RuntimeException {

    public FieldCryptoException(String message, Throwable cause) {
        super(message, cause);
    }

    public FieldCryptoException(String message) {
        super(message);
    }
}
