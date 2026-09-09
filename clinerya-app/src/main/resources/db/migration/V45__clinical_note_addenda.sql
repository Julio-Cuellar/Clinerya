-- ============================================================
-- V45 - Clinical note addenda (enmiendas NOM-004)
-- Una nota firmada nunca se modifica ni se borra. Toda correccion posterior
-- se guarda como un addendum aparte, con su propio hash y firma en
-- records.document_signatures. El contenido va cifrado (AesCryptoConverter).
-- ============================================================

CREATE TABLE IF NOT EXISTS records.clinical_note_addenda (
    id uuid NOT NULL,
    clinical_note_id uuid NOT NULL,
    patient_id uuid NOT NULL,
    clinic_id uuid NOT NULL,
    content text NOT NULL,
    created_by_user_id uuid NOT NULL,
    created_by_user_name varchar(255) NOT NULL,
    ip_address varchar(64),
    user_agent varchar(512),
    created_at timestamp(6) NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_clinical_note_addenda_note
    ON records.clinical_note_addenda (clinical_note_id, created_at);

CREATE INDEX IF NOT EXISTS idx_clinical_note_addenda_patient
    ON records.clinical_note_addenda (patient_id, clinic_id);
