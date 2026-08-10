-- ============================================================
-- core schema - Google Calendar integration (per-staff, one-way sync)
-- ============================================================

CREATE TABLE IF NOT EXISTS core.staff_calendar_credentials (
    id uuid NOT NULL,
    clinic_id uuid NOT NULL,
    staff_id uuid NOT NULL,
    google_account_email varchar(255) NOT NULL,
    access_token_enc TEXT NOT NULL,
    refresh_token_enc TEXT NOT NULL,
    token_expiry timestamp(6) NOT NULL,
    google_calendar_id varchar(255) NOT NULL DEFAULT 'primary',
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (clinic_id, staff_id)
);

ALTER TABLE core.appointments
    ADD COLUMN IF NOT EXISTS external_calendar_event_id varchar(255);
