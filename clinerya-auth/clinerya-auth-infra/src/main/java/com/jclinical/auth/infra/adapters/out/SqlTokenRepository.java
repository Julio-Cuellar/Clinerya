package com.jclinical.auth.infra.adapters.out;

import com.jclinical.auth.domain.ports.out.TokenRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HexFormat;

/**
 * Blacklist de tokens respaldada en base de datos.
 *
 * <p>Sustituye a la implementación en memoria: aquella perdía todos los logouts en cada
 * reinicio y no se compartiría entre instancias, de modo que un token robado seguía
 * siendo válido hasta expirar por su cuenta.
 *
 * <p>Se persiste el SHA-256 del token, no el token: la tabla no debe entregar
 * credenciales utilizables a quien logre leerla.
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class SqlTokenRepository implements TokenRepositoryPort {

    private final SpringDataRevokedTokenRepository repository;

    @Override
    @Transactional
    public void blacklistToken(String token, long expirationTimeMs) {
        if (token == null || token.isBlank() || expirationTimeMs <= 0) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        RevokedTokenEntity entity = new RevokedTokenEntity(
                hash(token),
                LocalDateTime.ofInstant(Instant.now().plusMillis(expirationTimeMs), ZoneId.systemDefault()),
                now);
        repository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isBlacklisted(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        return repository.existsByTokenHashAndExpiresAtAfter(hash(token), LocalDateTime.now());
    }

    /**
     * Las entradas expiradas ya no aportan nada: el token es rechazado por su propia
     * expiración. Se barren para que la tabla no crezca sin límite.
     */
    @Scheduled(fixedRate = 60 * 60 * 1000L)
    @Transactional
    public void purgeExpired() {
        int removed = repository.deleteExpired(LocalDateTime.now());
        if (removed > 0) {
            log.debug("Barrido de blacklist: {} tokens expirados eliminados", removed);
        }
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 no está disponible.", exception);
        }
    }
}
