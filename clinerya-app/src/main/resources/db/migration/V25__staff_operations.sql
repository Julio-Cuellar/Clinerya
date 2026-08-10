CREATE TABLE IF NOT EXISTS staff.staff_attendance_entries (
    id UUID PRIMARY KEY,
    clinic_id UUID NOT NULL,
    staff_id UUID NOT NULL,
    work_date DATE NOT NULL,
    clock_in_at TIMESTAMP NOT NULL,
    clock_out_at TIMESTAMP,
    status VARCHAR(20) NOT NULL,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_staff_attendance_staff
        FOREIGN KEY (staff_id) REFERENCES staff.clinic_staff(id)
);

CREATE INDEX IF NOT EXISTS idx_staff_attendance_clinic_date
    ON staff.staff_attendance_entries (clinic_id, work_date DESC, staff_id);

CREATE UNIQUE INDEX IF NOT EXISTS ux_staff_attendance_open
    ON staff.staff_attendance_entries (clinic_id, staff_id)
    WHERE status = 'OPEN';

CREATE TABLE IF NOT EXISTS staff.staff_activity_logs (
    id UUID PRIMARY KEY,
    clinic_id UUID NOT NULL,
    staff_id UUID NOT NULL,
    type VARCHAR(40) NOT NULL,
    reference_type VARCHAR(80),
    reference_id UUID,
    description TEXT,
    amount NUMERIC(14, 2) NOT NULL DEFAULT 0,
    occurred_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_staff_activity_staff
        FOREIGN KEY (staff_id) REFERENCES staff.clinic_staff(id)
);

CREATE INDEX IF NOT EXISTS idx_staff_activity_clinic_occurred
    ON staff.staff_activity_logs (clinic_id, occurred_at DESC, staff_id);

CREATE INDEX IF NOT EXISTS idx_staff_activity_reference
    ON staff.staff_activity_logs (reference_type, reference_id);

CREATE TABLE IF NOT EXISTS staff.staff_payroll_periods (
    id UUID PRIMARY KEY,
    clinic_id UUID NOT NULL,
    name VARCHAR(160) NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    gross_amount NUMERIC(14, 2) NOT NULL DEFAULT 0,
    net_amount NUMERIC(14, 2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at TIMESTAMP,
    CONSTRAINT chk_staff_payroll_period_range CHECK (period_end >= period_start)
);

CREATE INDEX IF NOT EXISTS idx_staff_payroll_periods_clinic_range
    ON staff.staff_payroll_periods (clinic_id, period_start DESC, period_end DESC);

CREATE TABLE IF NOT EXISTS staff.staff_payroll_lines (
    id UUID PRIMARY KEY,
    clinic_id UUID NOT NULL,
    payroll_period_id UUID NOT NULL,
    staff_id UUID NOT NULL,
    base_salary NUMERIC(14, 2) NOT NULL DEFAULT 0,
    commission_amount NUMERIC(14, 2) NOT NULL DEFAULT 0,
    bonus_amount NUMERIC(14, 2) NOT NULL DEFAULT 0,
    deduction_amount NUMERIC(14, 2) NOT NULL DEFAULT 0,
    gross_amount NUMERIC(14, 2) NOT NULL DEFAULT 0,
    net_amount NUMERIC(14, 2) NOT NULL DEFAULT 0,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_staff_payroll_line_period
        FOREIGN KEY (payroll_period_id) REFERENCES staff.staff_payroll_periods(id),
    CONSTRAINT fk_staff_payroll_line_staff
        FOREIGN KEY (staff_id) REFERENCES staff.clinic_staff(id),
    CONSTRAINT ux_staff_payroll_line_period_staff UNIQUE (payroll_period_id, staff_id)
);

CREATE INDEX IF NOT EXISTS idx_staff_payroll_lines_period
    ON staff.staff_payroll_lines (payroll_period_id, staff_id);
