-- ============================================================
-- V43 - Patient clinical data (allergies, conditions, active medications)
-- Datos clínicos tipados y consultables, junto al answers_json libre.
-- ============================================================

CREATE TABLE IF NOT EXISTS records.patient_allergies (
    id uuid NOT NULL,
    clinic_id uuid NOT NULL,
    patient_id uuid NOT NULL,
    substance varchar(255) NOT NULL,
    reaction varchar(255),
    severity varchar(16) NOT NULL DEFAULT 'UNKNOWN',
    category varchar(16) NOT NULL DEFAULT 'DRUG',
    source varchar(16) NOT NULL DEFAULT 'MANUAL',
    noted_by_user_id uuid,
    noted_by_user_name varchar(255),
    noted_at timestamp(6) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS records.patient_conditions (
    id uuid NOT NULL,
    clinic_id uuid NOT NULL,
    patient_id uuid NOT NULL,
    name varchar(255) NOT NULL,
    icd10_code varchar(16),
    status varchar(16) NOT NULL DEFAULT 'ACTIVE',
    onset_date date,
    source varchar(16) NOT NULL DEFAULT 'MANUAL',
    noted_by_user_id uuid,
    noted_by_user_name varchar(255),
    noted_at timestamp(6) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS records.patient_medications (
    id uuid NOT NULL,
    clinic_id uuid NOT NULL,
    patient_id uuid NOT NULL,
    medication_name varchar(255) NOT NULL,
    dose varchar(128),
    schedule varchar(128),
    active boolean NOT NULL DEFAULT true,
    started_on date,
    stopped_on date,
    prescription_id uuid,
    source varchar(16) NOT NULL DEFAULT 'MANUAL',
    noted_by_user_id uuid,
    noted_by_user_name varchar(255),
    noted_at timestamp(6) NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_patient_allergies_clinic_patient
    ON records.patient_allergies (clinic_id, patient_id);
CREATE INDEX IF NOT EXISTS idx_patient_conditions_clinic_patient
    ON records.patient_conditions (clinic_id, patient_id);
CREATE INDEX IF NOT EXISTS idx_patient_medications_clinic_patient
    ON records.patient_medications (clinic_id, patient_id);
