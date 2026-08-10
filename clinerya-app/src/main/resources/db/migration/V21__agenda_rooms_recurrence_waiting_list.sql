-- Migration V21: Add clinic_rooms, appointments room/series/cancellation columns, and waiting_list table

CREATE TABLE IF NOT EXISTS core.clinic_rooms (
    id uuid NOT NULL,
    clinic_id uuid NOT NULL,
    name varchar(255) NOT NULL,
    code varchar(64),
    color_hex varchar(32),
    description TEXT,
    active boolean NOT NULL DEFAULT true,
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_clinic_rooms_clinic ON core.clinic_rooms(clinic_id);

ALTER TABLE core.appointments
    ADD COLUMN IF NOT EXISTS room_id uuid,
    ADD COLUMN IF NOT EXISTS series_id uuid,
    ADD COLUMN IF NOT EXISTS cancellation_reason varchar(500),
    ADD COLUMN IF NOT EXISTS cancelled_at timestamp(6),
    ADD COLUMN IF NOT EXISTS cancelled_by_user_id uuid;

CREATE INDEX IF NOT EXISTS idx_appointments_room ON core.appointments(clinic_id, room_id);

CREATE TABLE IF NOT EXISTS core.waiting_list (
    id uuid NOT NULL,
    clinic_id uuid NOT NULL,
    patient_id uuid NOT NULL,
    doctor_staff_id uuid,
    room_id uuid,
    preferred_date_from date,
    preferred_date_to date,
    preferred_time_range varchar(64),
    notes TEXT,
    status varchar(32) NOT NULL DEFAULT 'WAITING',
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_waiting_list_clinic_status ON core.waiting_list(clinic_id, status);
