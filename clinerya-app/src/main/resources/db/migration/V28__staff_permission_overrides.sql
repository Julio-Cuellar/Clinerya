CREATE TABLE IF NOT EXISTS staff.staff_permission_overrides (
    id UUID PRIMARY KEY,
    clinic_id UUID NOT NULL,
    staff_id UUID NOT NULL,
    permission_code VARCHAR(80) NOT NULL,
    state VARCHAR(20) NOT NULL CHECK (state IN ('GRANTED', 'REVOKED')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_staff_permission_override_staff
        FOREIGN KEY (staff_id) REFERENCES staff.clinic_staff(id),
    CONSTRAINT ux_staff_permission_override
        UNIQUE (clinic_id, staff_id, permission_code)
);

CREATE INDEX IF NOT EXISTS idx_staff_permission_overrides_staff
    ON staff.staff_permission_overrides (clinic_id, staff_id);
