package com.jclinical.automation.domain.model;

/**
 * Firma X-Hub-Signature-256 de Meta: "sha256=" + HMAC-SHA256 hexadecimal del cuerpo crudo con el
 * secreto de la app. La comparacion es de tiempo constante.
 */
public final class WebhookSignature {

    private WebhookSignature() {
    }

    public static boolean isValid(byte[] rawBody, String header, String appSecret) {
        throw new UnsupportedOperationException("pendiente");
    }

    public static String sign(byte[] rawBody, String appSecret) {
        throw new UnsupportedOperationException("pendiente");
    }
}
