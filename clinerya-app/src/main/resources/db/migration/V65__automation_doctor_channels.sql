-- Entrega 5.6 (D8): celular donde el medico recibe avisos de solicitudes por WhatsApp. Solo recibe;
-- responde en Clinerya. El numero se guarda como WhatsApp identifica al remitente (521...).
CREATE TABLE IF NOT EXISTS automation.doctor_channels (
    clinic_id   uuid        NOT NULL,
    staff_id    uuid        NOT NULL,
    phone       varchar(20) NOT NULL,
    active      boolean     NOT NULL DEFAULT false,
    consent_at  timestamp,
    updated_by  uuid,
    updated_at  timestamp   NOT NULL,
    PRIMARY KEY (clinic_id, staff_id),
    CONSTRAINT ck_doctor_channels_consent CHECK (NOT active OR consent_at IS NOT NULL)
);

-- Reconocer al medico cuando escribe al numero de la clinica.
CREATE INDEX IF NOT EXISTS idx_doctor_channels_active_phone
    ON automation.doctor_channels (clinic_id, phone)
    WHERE active;

-- Recordatorio unico al medico si la solicitud sigue sin respuesta a las 4 h.
ALTER TABLE automation.appointment_requests
    ADD COLUMN IF NOT EXISTS reminded_at timestamp;

CREATE INDEX IF NOT EXISTS idx_appointment_requests_to_remind
    ON automation.appointment_requests (created_at)
    WHERE status = 'PENDING' AND reminded_at IS NULL;
