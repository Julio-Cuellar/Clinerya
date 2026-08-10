CREATE TABLE IF NOT EXISTS core.privacy_consents (
    id uuid NOT NULL,
    patient_id uuid NOT NULL,
    clinic_id uuid NOT NULL,
    privacy_notice_text text NOT NULL,
    document_hash varchar(128) NOT NULL,
    signer_name varchar(255) NOT NULL,
    signature_image text NOT NULL, -- Imagen base64 de la firma
    signature_image_hash varchar(128) NOT NULL,
    ip_address varchar(64),
    user_agent varchar(512),
    signed_at timestamp(6) NOT NULL,
    created_at timestamp(6) NOT NULL,
    PRIMARY KEY (id)
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_privacy_consents_patient_clinic ON core.privacy_consents (patient_id, clinic_id);
