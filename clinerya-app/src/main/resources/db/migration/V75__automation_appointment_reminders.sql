-- Plan v2, S6: recordatorio de cita por WhatsApp. Uno por cita: lo programan los eventos de la agenda
-- (agendada, reprogramada, cancelada, eliminada) y lo envia un barrido una sola vez.
CREATE TABLE IF NOT EXISTS automation.appointment_reminders (
    appointment_id  uuid PRIMARY KEY,
    clinic_id       uuid         NOT NULL,
    patient_id      uuid         NOT NULL,
    doctor_staff_id uuid,
    starts_at       timestamp    NOT NULL,
    service_name    varchar(200),
    send_at         timestamp    NOT NULL,
    sent_at         timestamp,
    updated_at      timestamp    NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_appointment_reminders_due
    ON automation.appointment_reminders (send_at)
    WHERE sent_at IS NULL;

-- Ajustes del recordatorio junto a los del asistente: encendido por defecto, 24 h antes, plantilla propia.
ALTER TABLE automation.clinic_channel_settings
    ADD COLUMN IF NOT EXISTS reminders_enabled boolean NOT NULL DEFAULT true,
    ADD COLUMN IF NOT EXISTS reminder_hours_before integer NOT NULL DEFAULT 24,
    ADD COLUMN IF NOT EXISTS reminder_template_name varchar(512);

ALTER TABLE automation.clinic_channel_settings
    DROP CONSTRAINT IF EXISTS chk_reminder_hours_before;
ALTER TABLE automation.clinic_channel_settings
    ADD CONSTRAINT chk_reminder_hours_before CHECK (reminder_hours_before BETWEEN 1 AND 72);

-- Un mensaje puede llevar su propia plantilla aprobada (el recordatorio, con sus botones).
ALTER TABLE automation.outbound_messages ADD COLUMN IF NOT EXISTS template_name varchar(512);
