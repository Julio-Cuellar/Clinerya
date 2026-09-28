-- Agente conversacional (plan agente-conversacional, fase F).

-- Accion con efecto real que el agente propuso y espera la confirmacion del paciente en un mensaje
-- posterior (a lo mas una por conversacion). El nombre del paciente y el motivo que dio van cifrados
-- (FieldCipher). Vence a los 30 minutos; lo vencido se borra en el barrido diario.
CREATE TABLE IF NOT EXISTS automation.agent_pending_actions (
    conversation_id  uuid PRIMARY KEY REFERENCES automation.conversations (id) ON DELETE CASCADE,
    kind             varchar(20)  NOT NULL,
    patient_id       uuid,
    patient_name     text,
    doctor_staff_id  uuid,
    doctor_name      varchar(200),
    start_at         timestamp,
    end_at           timestamp,
    appointment_id   uuid,
    note             text,
    proposed_at      timestamp    NOT NULL,
    CONSTRAINT ck_agent_pending_actions_kind
        CHECK (kind IN ('BOOK', 'CANCEL', 'RESCHEDULE', 'CONSENT', 'CONSENT_ACCEPTED'))
);

CREATE INDEX IF NOT EXISTS idx_agent_pending_actions_proposed ON automation.agent_pending_actions (proposed_at);

-- Reprogramar: la cita original se cancela solo cuando el medico aprueba la nueva.
ALTER TABLE automation.appointment_requests
    ADD COLUMN IF NOT EXISTS replaces_appointment_id uuid;

-- Perfil del asistente que configura cada clinica: nombre (vacio: habla a nombre de la clinica),
-- preguntas frecuentes y si comparte precios de lista.
ALTER TABLE automation.clinic_channel_settings
    ADD COLUMN IF NOT EXISTS assistant_name varchar(60),
    ADD COLUMN IF NOT EXISTS assistant_faq  text,
    ADD COLUMN IF NOT EXISTS show_prices    boolean NOT NULL DEFAULT false;
