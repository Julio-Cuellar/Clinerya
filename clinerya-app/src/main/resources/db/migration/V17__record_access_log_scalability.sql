-- ============================================================
-- V17 - Scalable record access audit log
-- ============================================================

DROP INDEX IF EXISTS core.idx_record_access_logs_patient;

CREATE INDEX IF NOT EXISTS idx_record_access_logs_patient_created_id
    ON core.record_access_logs (patient_id, created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_record_access_logs_clinic_created_id
    ON core.record_access_logs (clinic_id, created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_record_access_logs_created_at_brin
    ON core.record_access_logs USING brin (created_at) WITH (pages_per_range = 64);

ALTER TABLE core.record_access_logs SET (
    autovacuum_vacuum_scale_factor = 0.05,
    autovacuum_analyze_scale_factor = 0.02
);

CREATE TABLE IF NOT EXISTS core.record_access_log_outbox (
    id uuid NOT NULL,
    clinic_id uuid NOT NULL,
    patient_id uuid NOT NULL,
    user_id uuid NOT NULL,
    user_name varchar(255) NOT NULL,
    resource_type varchar(64) NOT NULL,
    resource_id uuid,
    action_type varchar(32) NOT NULL,
    ip_address varchar(64),
    user_agent varchar(512),
    created_at timestamp(6) NOT NULL,
    enqueued_at timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_record_access_log_outbox_enqueued
    ON core.record_access_log_outbox (enqueued_at, id);
