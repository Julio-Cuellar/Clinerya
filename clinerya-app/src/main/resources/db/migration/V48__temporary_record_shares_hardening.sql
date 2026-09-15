-- ============================================================
-- V48 - Endurecimiento de enlaces de consulta compartida
-- El endpoint publico /api/v1/public/shared-history devolvia sin autenticar
-- nombre, CURP, telefono, email y TODAS las notas clinicas de un paciente.
--
-- Cambios:
--   * Se guarda solo el hash SHA-256 del token (base64url), no el token: quien
--     lea esta tabla no debe obtener un enlace utilizable. Mismo criterio que
--     core.revoked_tokens y el password reset.
--   * revoked_at: el emisor puede invalidar un enlace antes de que expire.
--   * created_by_user_id: para la pantalla de gestion (listar/revocar).
--   * last_accessed_at / access_count: senal ligera de uso; la bitacora real
--     de cada consulta va a records.record_access_log.
--   * recipient_verified_at: reservado para la verificacion por codigo de un
--     solo uso enviado al correo destinatario (pendiente, requiere frontend).
--
-- Los enlaces vigentes se eliminan: son efimeros (<=30 dias), de bajo volumen,
-- y no se puede derivar su hash sin el token en claro. Los destinatarios
-- afectados solicitan un enlace nuevo.
-- ============================================================

DELETE FROM records.temporary_record_shares;

ALTER TABLE records.temporary_record_shares DROP COLUMN token;

ALTER TABLE records.temporary_record_shares
    ADD COLUMN token_hash varchar(255) NOT NULL,
    ADD COLUMN created_by_user_id uuid,
    ADD COLUMN revoked_at timestamp(6),
    ADD COLUMN recipient_verified_at timestamp(6),
    ADD COLUMN last_accessed_at timestamp(6),
    ADD COLUMN access_count integer NOT NULL DEFAULT 0;

ALTER TABLE records.temporary_record_shares
    ADD CONSTRAINT uq_temporary_record_shares_token_hash UNIQUE (token_hash);

CREATE INDEX IF NOT EXISTS idx_temporary_record_shares_patient
    ON records.temporary_record_shares (clinic_id, patient_id)
    WHERE revoked_at IS NULL;
