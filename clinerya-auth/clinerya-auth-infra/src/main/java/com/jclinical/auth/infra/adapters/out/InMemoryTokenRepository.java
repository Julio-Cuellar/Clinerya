package com.jclinical.auth.infra.adapters.out;

import com.jclinical.auth.domain.ports.out.TokenRepositoryPort;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryTokenRepository implements TokenRepositoryPort {

    private final Map<String, Long> blacklistedTokens = new ConcurrentHashMap<>();

    @Override
    public void blacklistToken(String token, long expirationTimeMs) {
        blacklistedTokens.put(token, System.currentTimeMillis() + expirationTimeMs);
    }

    @Override
    public boolean isBlacklisted(String token) {
        Long expiration = blacklistedTokens.get(token);
        if (expiration == null) {
            return false;
        }
        if (System.currentTimeMillis() > expiration) {
            blacklistedTokens.remove(token);
            return false;
        }
        return true;
    }
}
