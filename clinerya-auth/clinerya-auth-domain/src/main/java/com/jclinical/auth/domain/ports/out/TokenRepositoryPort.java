package com.jclinical.auth.domain.ports.out;

public interface TokenRepositoryPort {
    void blacklistToken(String token, long expirationTimeMs);
    boolean isBlacklisted(String token);
}
