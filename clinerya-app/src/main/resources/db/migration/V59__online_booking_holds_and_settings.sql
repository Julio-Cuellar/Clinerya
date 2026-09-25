-- Entrega 3 del flujo de citas por WhatsApp: cupos en linea y apartado temporal.
--
-- slot_holds: cupo apartado mientras el medico decide una solicitud. Mientras status = 'ACTIVE' y
-- expires_at no haya pasado, la agenda no deja agendar encima (ni el personal: decision de la
-- entrega 3). reference es el id de la solicitud que lo aparto. Dos apartados del mismo medico se
-- serializan con pg_advisory_xact_lock en la aplicacion, no con una restriccion de la tabla.
--
-- online_booking_settings / doctor_booking_preferences: reglas configurables. Sin fila aplican los
-- valores por defecto de la aplicacion (cupo de 30 min, anticipacion minima de 2 h).
CREATE TABLE IF NOT EXISTS agenda.slot_holds (
    id              uuid PRIMARY KEY,
    clinic_id       uuid        NOT NULL,
    doctor_staff_id uuid        NOT NULL,
    start_at        timestamp   NOT NULL,
    end_at          timestamp   NOT NULL,
    expires_at      timestamp   NOT NULL,
    reference       uuid        NOT NULL,
    status          varchar(20) NOT NULL,
    created_at      timestamp   NOT NULL,
    CONSTRAINT ck_slot_holds_range CHECK (start_at < end_at)
);

CREATE INDEX IF NOT EXISTS idx_slot_holds_active_doctor
    ON agenda.slot_holds (doctor_staff_id, clinic_id, start_at)
    WHERE status = 'ACTIVE';

CREATE TABLE IF NOT EXISTS agenda.online_booking_settings (
    clinic_id    uuid PRIMARY KEY,
    slot_minutes integer   NOT NULL,
    updated_at   timestamp NOT NULL
);

CREATE TABLE IF NOT EXISTS agenda.doctor_booking_preferences (
    clinic_id        uuid      NOT NULL,
    doctor_staff_id  uuid      NOT NULL,
    min_lead_minutes integer   NOT NULL,
    updated_at       timestamp NOT NULL,
    PRIMARY KEY (clinic_id, doctor_staff_id)
);
