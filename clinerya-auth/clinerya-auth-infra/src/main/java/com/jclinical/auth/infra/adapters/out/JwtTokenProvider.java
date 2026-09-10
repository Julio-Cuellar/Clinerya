package com.jclinical.auth.infra.adapters.out;

import com.jclinical.auth.domain.ports.out.TokenProviderPort;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider implements TokenProviderPort {

    /**
     * Todo token lleva su tipo declarado y la validación exige el tipo esperado. Sin esto,
     * firma y expiración son lo único que se comprueba y un refresh token (7 días) sirve
     * como bearer de acceso, o un access token sirve para renovar indefinidamente.
     */
    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_ACCESS = "ACCESS";
    private static final String TYPE_REFRESH = "REFRESH";

    private final SecretKey key;
    private final long validityInMilliseconds;
    private final long refreshValidityInMilliseconds;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration:1800000}") long validityInMilliseconds,
            @Value("${jwt.refresh-expiration:604800000}") long refreshValidityInMilliseconds) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 48) {
            throw new IllegalStateException(
                    "jwt.secret no está configurado o es demasiado corto (mínimo 48 bytes para HS384). "
                            + "Define un secreto aleatorio robusto vía la propiedad jwt.secret antes de arrancar la aplicación.");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.validityInMilliseconds = validityInMilliseconds;
        this.refreshValidityInMilliseconds = refreshValidityInMilliseconds;
    }

    @Override
    public String createToken(UUID userId, String email) {
        return build(userId, email, TYPE_ACCESS, validityInMilliseconds);
    }

    @Override
    public String createRefreshToken(UUID userId, String email) {
        return build(userId, email, TYPE_REFRESH, refreshValidityInMilliseconds);
    }

    private String build(UUID userId, String email, String type, long ttlMillis) {
        Date now = new Date();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim(CLAIM_TYPE, type)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttlMillis))
                .signWith(key)
                .compact();
    }

    @Override
    public boolean validateAccessToken(String token) {
        return hasType(token, TYPE_ACCESS);
    }

    @Override
    public boolean validateRefreshToken(String token) {
        return hasType(token, TYPE_REFRESH);
    }

    private boolean hasType(String token, String expectedType) {
        try {
            return expectedType.equals(parseClaims(token).get(CLAIM_TYPE, String.class));
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getUserIdFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    @Override
    public String getEmailFromToken(String token) {
        return parseClaims(token).get("email", String.class);
    }

    @Override
    public long millisUntilExpiry(String token) {
        try {
            Date expiration = parseClaims(token).getExpiration();
            if (expiration == null) {
                return 0L;
            }
            return Math.max(0L, expiration.getTime() - System.currentTimeMillis());
        } catch (Exception e) {
            return 0L;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
