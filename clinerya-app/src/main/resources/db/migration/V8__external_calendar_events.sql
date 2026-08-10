ALTER TABLE core.staff_calendar_credentials
    ADD COLUMN IF NOT EXISTS calendar_sync_token TEXT;

CREATE TABLE IF NOT EXISTS core.external_calendar_events (
    id uuid NOT NULL,
    clinic_id uuid NOT NULL,
    staff_id uuid NOT NULL,
    google_event_id varchar(255) NOT NULL,
    summary varchar(500),
    description TEXT,
    start_time timestamp(6) NOT NULL,
    end_time timestamp(6) NOT NULL,
    status varchar(20) NOT NULL CHECK (status IN ('PENDING_REVIEW','LINKED','DISMISSED')),
    linked_appointment_id uuid,
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (clinic_id, google_event_id)
);
