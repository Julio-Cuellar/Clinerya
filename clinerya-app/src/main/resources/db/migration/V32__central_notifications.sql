CREATE SCHEMA IF NOT EXISTS notifications;

CREATE TABLE IF NOT EXISTS notifications.notifications (
    id uuid PRIMARY KEY,
    clinic_id uuid NOT NULL,
    source_module varchar(60) NOT NULL,
    source_key varchar(180) NOT NULL,
    severity varchar(20) NOT NULL,
    title varchar(180) NOT NULL,
    message text NOT NULL,
    action_path varchar(240),
    created_at timestamp without time zone NOT NULL,
    expires_at timestamp without time zone,
    read_at timestamp without time zone,
    CONSTRAINT uq_notifications_source UNIQUE (clinic_id, source_module, source_key)
);

CREATE INDEX IF NOT EXISTS idx_notifications_clinic_created
    ON notifications.notifications (clinic_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_notifications_clinic_unread
    ON notifications.notifications (clinic_id, read_at)
    WHERE read_at IS NULL;
