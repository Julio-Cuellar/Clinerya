package com.jclinical.core.security;

/**
 * El destinatario de un enlace compartido aun no confirma su correo con el codigo
 * de un solo uso. Se distingue de un token invalido (400) porque el frontend debe
 * mostrar la pantalla de verificacion en lugar de un error.
 */
public class RecipientVerificationRequiredException extends RuntimeException {

    public RecipientVerificationRequiredException(String message) {
        super(message);
    }
}
