CREATE TABLE IF NOT EXISTS clinics.clinic_room_staff_assignments (
    id UUID PRIMARY KEY,
    clinic_id UUID NOT NULL,
    room_id UUID NOT NULL,
    staff_id UUID NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    unassigned_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_clinic_room_staff_assignment_room
        FOREIGN KEY (room_id) REFERENCES clinics.clinic_rooms(id),
    CONSTRAINT ux_clinic_room_staff_assignment
        UNIQUE (clinic_id, room_id, staff_id)
);

CREATE INDEX IF NOT EXISTS idx_clinic_room_staff_assignment_room
    ON clinics.clinic_room_staff_assignments (clinic_id, room_id, active);

CREATE INDEX IF NOT EXISTS idx_clinic_room_staff_assignment_staff
    ON clinics.clinic_room_staff_assignments (clinic_id, staff_id, active);
