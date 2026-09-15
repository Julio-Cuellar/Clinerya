package com.jclinical.auth.domain.service;

import com.jclinical.auth.domain.ports.out.TokenProviderPort;
import com.jclinical.auth.domain.ports.out.TokenRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cubre los cruces entre tipos de token y la interaccion con la blacklist:
 * un refresh no debe servir como access, un access no debe renovar, y el logout
 * debe cerrar de verdad la sesion (incluido el refresh).
 */
class TokenTypeAndBlacklistTest {

    private static final long ACCESS_TTL = 60_000L;
    private static final long REFRESH_TTL = 604_800_000L;

    private FakeTokenProvider tokenProvider;
    private FakeTokenRepository tokenRepository;
    private RefreshTokenService refreshService;
    private LogoutService logoutService;
    private ValidateTokenService validateService;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        tokenProvider = new FakeTokenProvider();
        tokenRepository = new FakeTokenRepository();
        refreshService = new RefreshTokenService(tokenProvider, tokenRepository);
        logoutService = new LogoutService(tokenRepository, tokenProvider);
        validateService = new ValidateTokenService(tokenRepository);
    }

    @Test
    void rejectsAccessTokenUsedToRefresh() {
        String accessToken = tokenProvider.createToken(userId, "doctora@clinica.mx");

        IllegalArgumentException error =
                assertThrows(IllegalArgumentException.class, () -> refreshService.refresh(accessToken));
        assertEquals("Refresh token inválido o expirado", error.getMessage());
    }

    @Test
    void rejectsRefreshTokenUsedAsAccessToken() {
        String refreshToken = tokenProvider.createRefreshToken(userId, "doctora@clinica.mx");

        assertFalse(tokenProvider.validateAccessToken(refreshToken));
        assertTrue(tokenProvider.validateRefreshToken(refreshToken));
    }

    @Test
    void acceptsRefreshTokenAndMintsAnAccessToken() {
        String refreshToken = tokenProvider.createRefreshToken(userId, "doctora@clinica.mx");

        String issued = refreshService.refresh(refreshToken);

        assertTrue(tokenProvider.validateAccessToken(issued));
        assertFalse(tokenProvider.validateRefreshToken(issued));
    }

    @Test
    void rejectsBlacklistedRefreshToken() {
        String refreshToken = tokenProvider.createRefreshToken(userId, "doctora@clinica.mx");
        logoutService.logout(refreshToken);

        assertThrows(IllegalArgumentException.class, () -> refreshService.refresh(refreshToken));
    }

    @Test
    void logoutRetainsEachTokenForItsOwnRemainingLifetime() {
        String accessToken = tokenProvider.createToken(userId, "doctora@clinica.mx");
        String refreshToken = tokenProvider.createRefreshToken(userId, "doctora@clinica.mx");

        logoutService.logout(accessToken);
        logoutService.logout(refreshToken);

        // Un plazo fijo de 24 h dejaba revivir el refresh, que vive 7 dias.
        assertEquals(ACCESS_TTL, tokenRepository.retentionOf(accessToken));
        assertEquals(REFRESH_TTL, tokenRepository.retentionOf(refreshToken));
        assertFalse(validateService.validate(refreshToken));
    }

    @Test
    void logoutIgnoresExpiredOrUnparseableTokens() {
        logoutService.logout("no-es-un-token");
        logoutService.logout("   ");
        logoutService.logout(null);

        assertTrue(tokenRepository.isEmpty());
    }

    /** Simula la semantica de JwtTokenProvider sin depender de la libreria JWT. */
    private static final class FakeTokenProvider implements TokenProviderPort {
        private final Map<String, String> types = new HashMap<>();
        private final Map<String, Long> ttls = new HashMap<>();

        @Override
        public String createToken(UUID id, String email) {
            return register("access-" + UUID.randomUUID(), "ACCESS", ACCESS_TTL);
        }

        @Override
        public String createRefreshToken(UUID id, String email) {
            return register("refresh-" + UUID.randomUUID(), "REFRESH", REFRESH_TTL);
        }

        private String register(String token, String type, long ttl) {
            types.put(token, type);
            ttls.put(token, ttl);
            return token;
        }

        @Override
        public boolean validateAccessToken(String token) {
            return "ACCESS".equals(types.get(token));
        }

        @Override
        public boolean validateRefreshToken(String token) {
            return "REFRESH".equals(types.get(token));
        }

        @Override
        public String getUserIdFromToken(String token) {
            return UUID.randomUUID().toString();
        }

        @Override
        public String getEmailFromToken(String token) {
            return "doctora@clinica.mx";
        }

        @Override
        public long millisUntilExpiry(String token) {
            return ttls.getOrDefault(token, 0L);
        }
    }

    private static final class FakeTokenRepository implements TokenRepositoryPort {
        private final Map<String, Long> retentions = new HashMap<>();

        @Override
        public void blacklistToken(String token, long expirationTimeMs) {
            retentions.put(token, expirationTimeMs);
        }

        @Override
        public boolean isBlacklisted(String token) {
            return retentions.containsKey(token);
        }

        long retentionOf(String token) {
            return retentions.getOrDefault(token, -1L);
        }

        boolean isEmpty() {
            return retentions.isEmpty();
        }
    }
}
