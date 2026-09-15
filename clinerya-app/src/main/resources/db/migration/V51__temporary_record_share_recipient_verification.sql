-- Verificacion del destinatario de un enlace compartido por codigo de un solo uso
-- enviado al correo (recipient_verified_at, reservado desde V48, ahora se usa).
--
-- Solo se guarda el hash del codigo (mismo criterio que token_hash): quien lea esta
-- tabla no debe poder verificarse en lugar del destinatario real.
ALTER TABLE records.temporary_record_shares
    ADD COLUMN verification_code_hash varchar(255),
    ADD COLUMN verification_code_expires_at timestamp(6),
    ADD COLUMN verification_attempts integer NOT NULL DEFAULT 0;
