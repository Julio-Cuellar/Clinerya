-- CU-4: datos que un paciente nuevo va dando por WhatsApp antes de quedar registrado.
-- Nombre, apellidos y fecha de nacimiento van cifrados (FieldCipher). Se borran al registrar,
-- al reiniciar la conversacion o, si se abandonan, al dia siguiente.
CREATE TABLE IF NOT EXISTS automation.registration_drafts (
    conversation_id    uuid PRIMARY KEY REFERENCES automation.conversations (id) ON DELETE CASCADE,
    clinic_id          uuid         NOT NULL,
    consent_version    varchar(40)  NOT NULL,
    first_name         text,
    last_name_paterno  text,
    last_name_materno  text,
    date_of_birth      text,
    sex                varchar(10),
    updated_at         timestamp    NOT NULL,
    CONSTRAINT ck_registration_drafts_sex CHECK (sex IS NULL OR sex IN ('FEMALE', 'MALE', 'OTHER'))
);

CREATE INDEX IF NOT EXISTS idx_registration_drafts_updated ON automation.registration_drafts (updated_at);
