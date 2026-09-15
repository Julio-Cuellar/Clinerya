-- Blacklist de tokens persistente. Antes vivia en un ConcurrentHashMap: el logout se
-- perdia en cada reinicio y no se propagaria entre instancias, asi que un token robado
-- sobrevivia hasta su expiracion.
--
-- Se guarda el hash SHA-256 del token, no el token: quien lea esta tabla no debe obtener
-- credenciales utilizables.
CREATE TABLE IF NOT EXISTS core.revoked_tokens (
    token_hash VARCHAR(64) PRIMARY KEY,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Para el barrido periodico de entradas ya expiradas.
CREATE INDEX IF NOT EXISTS idx_revoked_tokens_expires_at
    ON core.revoked_tokens (expires_at);
