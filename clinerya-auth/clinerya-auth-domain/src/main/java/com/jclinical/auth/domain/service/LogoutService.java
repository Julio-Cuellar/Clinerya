package com.jclinical.auth.domain.service;

import com.jclinical.auth.domain.ports.in.LogoutUseCase;
import com.jclinical.auth.domain.ports.out.TokenRepositoryPort;

public class LogoutService implements LogoutUseCase {
    private final TokenRepositoryPort tokenRepository;

    public LogoutService(TokenRepositoryPort tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    @Override
    public void logout(String token) {
        if (token != null && !token.trim().isEmpty()) {
            tokenRepository.blacklistToken(token, 86400000L);
        }
    }
}
