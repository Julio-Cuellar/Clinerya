-- ============================================================
-- V44 - Patient clinical review status (por tipo de dato)
-- "Preguntadas y negadas" / "sin ... reportados": afirmacion explicita del
-- clinico, distinta de "nadie lo ha capturado". Aplica a alergias,
-- padecimientos y medicacion.
-- ============================================================

CREATE TABLE IF NOT EXISTS records.patient_clinical_reviews (
    id uuid NOT NULL,
    clinic_id uuid NOT NULL,
    patient_id uuid NOT NULL,
    kind varchar(16) NOT NULL,
    none_reported boolean NOT NULL DEFAULT false,
    reviewed_by_user_id uuid,
    reviewed_by_user_name varchar(255),
    reviewed_at timestamp(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_patient_clinical_reviews UNIQUE (clinic_id, patient_id, kind)
);
