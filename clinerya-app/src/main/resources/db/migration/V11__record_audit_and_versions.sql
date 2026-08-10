-- ============================================================
-- V11 - Record Audit and Versions
-- ============================================================

ALTER TABLE IF EXISTS core.medical_histories
    ADD COLUMN IF NOT EXISTS version integer NOT NULL DEFAULT 1;

CREATE TABLE IF NOT EXISTS core.record_access_logs (
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
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS core.medical_history_versions (
    id uuid NOT NULL,
    medical_history_id uuid NOT NULL,
    version integer NOT NULL,
    answers_json TEXT NOT NULL,
    changed_by_user_id uuid NOT NULL,
    changed_by_user_name varchar(255) NOT NULL,
    ip_address varchar(64),
    user_agent varchar(512),
    created_at timestamp(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (medical_history_id, version)
);

CREATE INDEX IF NOT EXISTS idx_record_access_logs_patient
    ON core.record_access_logs (patient_id);

CREATE INDEX IF NOT EXISTS idx_medical_history_versions_history
    ON core.medical_history_versions (medical_history_id);
