ALTER TABLE users.users
    ADD COLUMN password_reset_token_hash VARCHAR(64),
    ADD COLUMN password_reset_token_expires_at TIMESTAMP;

CREATE INDEX idx_users_password_reset_token_hash
    ON users.users (password_reset_token_hash)
    WHERE password_reset_token_hash IS NOT NULL;
