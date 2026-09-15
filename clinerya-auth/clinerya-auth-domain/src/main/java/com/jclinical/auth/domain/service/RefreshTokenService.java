package com.jclinical.auth.domain.service;

import com.jclinical.auth.domain.ports.in.RefreshTokenUseCase;
import com.jclinical.auth.domain.ports.out.TokenProviderPort;
import com.jclinical.auth.domain.ports.out.TokenRepositoryPort;
import java.util.UUID;

public class RefreshTokenService implements RefreshTokenUseCase {
    private final TokenProviderPort tokenProvider;
    private final TokenRepositoryPort tokenRepository;

    public RefreshTokenService(TokenProviderPort tokenProvider, TokenRepositoryPort tokenRepository) {
        this.tokenProvider = tokenProvider;
        this.tokenRepository = tokenRepository;
    }

    @Override
    public String refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            throw new IllegalArgumentException("El refresh token es obligatorio");
        }
        // Exige tipo REFRESH: un access token no puede renovarse a sí mismo indefinidamente.
        if (!tokenProvider.validateRefreshToken(refreshToken)) {
            throw new IllegalArgumentException("Refresh token inválido o expirado");
        }
        // Sin esto, un token al que ya se le hizo logout seguiría emitiendo tokens de acceso.
        if (tokenRepository.isBlacklisted(refreshToken)) {
            throw new IllegalArgumentException("Refresh token inválido o expirado");
        }
        String userId = tokenProvider.getUserIdFromToken(refreshToken);
        String email = tokenProvider.getEmailFromToken(refreshToken);
        return tokenProvider.createToken(UUID.fromString(userId), email);
    }
}
