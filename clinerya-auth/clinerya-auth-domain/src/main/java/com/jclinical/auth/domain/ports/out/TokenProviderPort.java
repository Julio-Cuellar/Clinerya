package com.jclinical.auth.domain.ports.out;

import java.util.UUID;

public interface TokenProviderPort {
    String createToken(UUID userId, String email);
    String createRefreshToken(UUID userId, String email);
    boolean validateToken(String token);
    String getUserIdFromToken(String token);
    String getEmailFromToken(String token);
}
