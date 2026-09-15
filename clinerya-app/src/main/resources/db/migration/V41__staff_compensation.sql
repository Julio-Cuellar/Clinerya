-- Compensacion base por empleado: alimenta la generacion de lineas de nomina
-- (P2) y evita recapturar el sueldo en cada periodo.
CREATE TABLE IF NOT EXISTS staff.staff_compensation (
    staff_id UUID PRIMARY KEY,
    clinic_id UUID NOT NULL,
    base_salary NUMERIC(14, 2) NOT NULL DEFAULT 0,
    pay_frequency VARCHAR(20) NOT NULL DEFAULT 'BIWEEKLY',
    payment_method VARCHAR(20) NOT NULL DEFAULT 'BANK_TRANSFER',
    payment_account_clabe VARCHAR(18),
    rfc VARCHAR(13),
    curp VARCHAR(18),
    nss VARCHAR(11),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_staff_compensation_staff
        FOREIGN KEY (staff_id) REFERENCES staff.clinic_staff(id),
    CONSTRAINT chk_staff_compensation_base_salary CHECK (base_salary >= 0)
);

CREATE INDEX IF NOT EXISTS idx_staff_compensation_clinic
    ON staff.staff_compensation (clinic_id);
