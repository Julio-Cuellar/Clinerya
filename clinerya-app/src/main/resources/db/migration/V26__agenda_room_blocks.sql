CREATE TABLE IF NOT EXISTS agenda.room_blocks (
    id UUID PRIMARY KEY,
    clinic_id UUID NOT NULL,
    room_id UUID NOT NULL,
    starts_at TIMESTAMP(6) NOT NULL,
    ends_at TIMESTAMP(6) NOT NULL,
    type VARCHAR(32) NOT NULL,
    reason TEXT,
    created_by_user_id UUID,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_agenda_room_blocks_range CHECK (starts_at < ends_at)
);

CREATE INDEX IF NOT EXISTS idx_agenda_room_blocks_room_range
    ON agenda.room_blocks (clinic_id, room_id, starts_at, ends_at);

CREATE INDEX IF NOT EXISTS idx_agenda_room_blocks_clinic_range
    ON agenda.room_blocks (clinic_id, starts_at, ends_at);
