-- Solicitudes de cita que un paciente hace por chat y que solo el medico asignado resuelve
-- (clinerya-automation, entrega 4). hold_id es el cupo apartado en la agenda mientras el medico
-- decide; proposed_options guarda, en JSON, los horarios que el medico propuso con su propio apartado.
-- No hay llave foranea hacia la agenda: los modulos solo se conocen por sus puertos.
CREATE TABLE IF NOT EXISTS automation.appointment_requests (
    id                uuid PRIMARY KEY,
    clinic_id         uuid         NOT NULL,
    conversation_id   uuid         NOT NULL REFERENCES automation.conversations (id),
    patient_id        uuid         NOT NULL,
    patient_name      varchar(200),
    patient_phone     varchar(20)  NOT NULL,
    doctor_staff_id   uuid         NOT NULL,
    doctor_name       varchar(200),
    start_at          timestamp    NOT NULL,
    end_at            timestamp    NOT NULL,
    hold_id           uuid         NOT NULL,
    status            varchar(20)  NOT NULL,
    proposed_options  text         NOT NULL DEFAULT '[]',
    appointment_id    uuid,
    rejection_reason  varchar(500),
    created_at        timestamp    NOT NULL,
    responded_at      timestamp,
    CONSTRAINT ck_appointment_requests_status
        CHECK (status IN ('PENDING', 'OPTIONS_PROPOSED', 'BOOKED', 'REJECTED', 'DECLINED', 'EXPIRED')),
    CONSTRAINT ck_appointment_requests_range CHECK (end_at > start_at)
);

-- Bandeja del medico: sus solicitudes pendientes, las mas antiguas primero.
CREATE INDEX IF NOT EXISTS idx_appointment_requests_pending_doctor
    ON automation.appointment_requests (clinic_id, doctor_staff_id, created_at)
    WHERE status = 'PENDING';

-- Barrido de vencimientos (24 h sin respuesta del medico o sin eleccion del paciente).
CREATE INDEX IF NOT EXISTS idx_appointment_requests_open
    ON automation.appointment_requests (status, created_at, responded_at)
    WHERE status IN ('PENDING', 'OPTIONS_PROPOSED');

-- Mensajes al paciente por iniciativa de la automatizacion (respuesta del medico, vencimiento).
-- Se escriben en la misma transaccion que mueven la conversacion; el canal (WhatsApp, entrega 5)
-- los envia y los marca SENT.
CREATE TABLE IF NOT EXISTS automation.outbound_messages (
    id          uuid PRIMARY KEY,
    clinic_id   uuid         NOT NULL,
    phone       varchar(20)  NOT NULL,
    body        text         NOT NULL,
    options     text         NOT NULL DEFAULT '[]',
    status      varchar(20)  NOT NULL DEFAULT 'PENDING',
    created_at  timestamp    NOT NULL,
    sent_at     timestamp,
    CONSTRAINT ck_outbound_messages_status CHECK (status IN ('PENDING', 'SENT', 'FAILED'))
);

CREATE INDEX IF NOT EXISTS idx_outbound_messages_pending
    ON automation.outbound_messages (created_at)
    WHERE status = 'PENDING';
