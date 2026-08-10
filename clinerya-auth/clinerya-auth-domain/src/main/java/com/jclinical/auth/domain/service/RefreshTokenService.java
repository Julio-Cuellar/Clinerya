package com.jclinical.auth.domain.service;

import com.jclinical.auth.domain.ports.in.RefreshTokenUseCase;
import com.jclinical.auth.domain.ports.out.TokenProviderPort;
import java.util.UUID;

public class RefreshTokenService implements RefreshTokenUseCase {
    private final TokenProviderPort tokenProvider;

    public RefreshTokenService(TokenProviderPort tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    public String refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            throw new IllegalArgumentException("El refresh token es obligatorio");
        }
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("Refresh token inválido o expirado");
        }
        String userId = tokenProvider.getUserIdFromToken(refreshToken);
        String email = tokenProvider.getEmailFromToken(refreshToken);
        return tokenProvider.createToken(UUID.fromString(userId), email);
    }
}
