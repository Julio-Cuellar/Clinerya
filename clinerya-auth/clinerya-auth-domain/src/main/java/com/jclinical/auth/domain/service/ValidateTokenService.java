package com.jclinical.auth.domain.service;

import com.jclinical.auth.domain.ports.in.ValidateTokenUseCase;
import com.jclinical.auth.domain.ports.out.TokenRepositoryPort;

public class ValidateTokenService implements ValidateTokenUseCase {
    private final TokenRepositoryPort tokenRepository;

    public ValidateTokenService(TokenRepositoryPort tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    @Override
    public boolean validate(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        return !tokenRepository.isBlacklisted(token);
    }
}
