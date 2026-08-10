ALTER TABLE IF EXISTS core.clinical_notes
    ADD COLUMN IF NOT EXISTS signed_at timestamp(6),
    ADD COLUMN IF NOT EXISTS signed_by_user_id uuid,
    ADD COLUMN IF NOT EXISTS document_hash varchar(128);

CREATE TABLE IF NOT EXISTS core.document_signatures (
    id uuid NOT NULL,
    document_type varchar(64) NOT NULL,
    document_id uuid NOT NULL,
    clinic_id uuid NOT NULL,
    patient_id uuid,
    signer_type varchar(32) NOT NULL,
    signer_user_id uuid,
    signer_name varchar(255) NOT NULL,
    signature_field_id varchar(255),
    signature_field_label varchar(255),
    signature_image_hash varchar(128),
    document_hash varchar(128) NOT NULL,
    ip_address varchar(64),
    user_agent varchar(512),
    status varchar(32) NOT NULL,
    signed_at timestamp(6) NOT NULL,
    created_at timestamp(6) NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_document_signatures_document
    ON core.document_signatures (document_type, document_id);

CREATE INDEX IF NOT EXISTS idx_document_signatures_patient
    ON core.document_signatures (patient_id);

CREATE INDEX IF NOT EXISTS idx_document_signatures_clinic
    ON core.document_signatures (clinic_id);
