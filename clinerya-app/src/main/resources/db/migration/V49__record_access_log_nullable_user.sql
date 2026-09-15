-- Las consultas a un expediente vía enlace compartido (TEMPORARY_SHARE) no tienen
-- un usuario interno autenticado: la identidad queda en user_name como
-- "enlace-compartido:<correo>". Permitir user_id NULL en la bitácora y su outbox.
ALTER TABLE records.record_access_logs ALTER COLUMN user_id DROP NOT NULL;
ALTER TABLE records.record_access_log_outbox ALTER COLUMN user_id DROP NOT NULL;
