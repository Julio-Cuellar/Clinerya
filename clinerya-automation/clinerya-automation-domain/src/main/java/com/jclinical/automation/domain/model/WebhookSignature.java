package com.jclinical.automation.domain.model;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Firma X-Hub-Signature-256 de Meta: "sha256=" + HMAC-SHA256 hexadecimal del cuerpo crudo con el
 * secreto de la app. La comparacion es de tiempo constante.
 */
public final class WebhookSignature {

    private static final String PREFIX = "sha256=";
    private static final String ALGORITHM = "HmacSHA256";

    private WebhookSignature() {
    }

    public static boolean isValid(byte[] rawBody, String header, String appSecret) {
        if (rawBody == null || header == null || appSecret == null || appSecret.isBlank()) {
            return false;
        }
        String received = header.trim().toLowerCase(Locale.ROOT);
        if (!received.startsWith(PREFIX)) {
            return false;
        }
        return MessageDigest.isEqual(sign(rawBody, appSecret).getBytes(StandardCharsets.US_ASCII),
                received.getBytes(StandardCharsets.US_ASCII));
    }

    public static String sign(byte[] rawBody, String appSecret) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            return PREFIX + HexFormat.of().formatHex(mac.doFinal(rawBody));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("No se pudo calcular la firma del webhook.", exception);
        }
    }
}
