package com.jclinical.auth.domain.ports.out;

import java.util.UUID;

public interface TokenProviderPort {
    String createToken(UUID userId, String email);
    String createRefreshToken(UUID userId, String email);
    /** Válido y además emitido como token de acceso (no un refresh reutilizado como bearer). */
    boolean validateAccessToken(String token);

    /** Válido y además emitido como refresh token (no un access token usado para renovar). */
    boolean validateRefreshToken(String token);
    String getUserIdFromToken(String token);
    String getEmailFromToken(String token);

    /**
     * Milisegundos que le quedan de vida al token; 0 si ya expiró o no es válido.
     * Permite que la blacklist retenga cada token exactamente lo que dura, en vez de un
     * plazo fijo que se queda corto para los refresh (7 días) o largo para los de acceso.
     */
    long millisUntilExpiry(String token);
}
