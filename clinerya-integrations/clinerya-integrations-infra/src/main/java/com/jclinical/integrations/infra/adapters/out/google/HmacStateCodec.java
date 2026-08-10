package com.jclinical.integrations.infra.adapters.out.google;

import com.jclinical.integrations.domain.ports.out.StateCodecPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Codifica clinicId+staffId en el parámetro "state" del flujo OAuth, firmado con HMAC-SHA256
 * para que el callback de Google (que llega sin el JWT de sesión de la app) pueda confiar en él.
 */
@Component
@Slf4j
public class HmacStateCodec implements StateCodecPort {

    private static final long MAX_AGE_MILLIS = 15 * 60 * 1000L;

    private final SecretKeySpec keySpec;

    public HmacStateCodec(@Value("${google.oauth.state-secret:***OAUTH_STATE_SECRET_DEFAULT_REMOVED***}") String secret) {
        this.keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Override
    public String encode(UUID clinicId, UUID staffId, boolean importPastEvents) {
        String payload = clinicId + ":" + staffId + ":" + importPastEvents + ":" + Instant.now().toEpochMilli();
        String signature = sign(payload);
        String encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return encodedPayload + "." + signature;
    }

    @Override
    public DecodedState decode(String state) {
        int separatorIndex = state.lastIndexOf('.');
        if (separatorIndex < 0) {
            throw new IllegalArgumentException("El parámetro state es inválido.");
        }
        String encodedPayload = state.substring(0, separatorIndex);
        String signature = state.substring(separatorIndex + 1);
        String payload = new String(Base64.getUrlDecoder().decode(encodedPayload), StandardCharsets.UTF_8);

        if (!sign(payload).equals(signature)) {
            throw new IllegalArgumentException("La firma del parámetro state no es válida.");
        }

        String[] parts = payload.split(":");
        if (parts.length != 4) {
            throw new IllegalArgumentException("El parámetro state es inválido.");
        }

        long issuedAt = Long.parseLong(parts[3]);
        if (Instant.now().toEpochMilli() - issuedAt > MAX_AGE_MILLIS) {
            throw new IllegalArgumentException("El enlace de conexión con Google expiró, intenta de nuevo.");
        }

        return new DecodedState(UUID.fromString(parts[0]), UUID.fromString(parts[1]), Boolean.parseBoolean(parts[2]));
    }


    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(keySpec);
            byte[] signed = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signed);
        } catch (Exception e) {
            log.error("Error firmando el parámetro state", e);
            throw new IllegalStateException("No se pudo firmar el parámetro state", e);
        }
    }
}
