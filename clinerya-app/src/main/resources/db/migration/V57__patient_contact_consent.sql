-- CU-1 del flujo de citas por WhatsApp: consentimiento del paciente para recibir mensajes por
-- iniciativa de la clinica (recordatorios, seguimiento). Se guarda la decision, la version del texto
-- que se le leyo, por que via se registro, quien la capturo y cuando; tambien el "no" explicito.
--
-- Los pacientes existentes quedan con granted = false y recorded_at nulo: nunca se les pregunto, asi
-- que ningun mensaje proactivo sale hasta que el personal registre su decision.
ALTER TABLE patients.patients
    ADD COLUMN IF NOT EXISTS contact_consent_granted      boolean NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS contact_consent_text_version varchar(40),
    ADD COLUMN IF NOT EXISTS contact_consent_source       varchar(40),
    ADD COLUMN IF NOT EXISTS contact_consent_recorded_by  uuid,
    ADD COLUMN IF NOT EXISTS contact_consent_recorded_at  timestamp;
