package com.jclinical.auth.domain.service;

import com.jclinical.auth.domain.ports.in.LogoutUseCase;
import com.jclinical.auth.domain.ports.out.TokenProviderPort;
import com.jclinical.auth.domain.ports.out.TokenRepositoryPort;

public class LogoutService implements LogoutUseCase {
    private final TokenRepositoryPort tokenRepository;
    private final TokenProviderPort tokenProvider;

    public LogoutService(TokenRepositoryPort tokenRepository, TokenProviderPort tokenProvider) {
        this.tokenRepository = tokenRepository;
        this.tokenProvider = tokenProvider;
    }

    @Override
    public void logout(String token) {
        if (token == null || token.trim().isEmpty()) {
            return;
        }
        // La retención se deriva del propio token: un plazo fijo de 24 h dejaba revivir
        // los refresh (7 días de vida) una vez vencida la entrada en la blacklist.
        long remaining = tokenProvider.millisUntilExpiry(token);
        if (remaining <= 0L) {
            return;
        }
        tokenRepository.blacklistToken(token, remaining);
    }
}
