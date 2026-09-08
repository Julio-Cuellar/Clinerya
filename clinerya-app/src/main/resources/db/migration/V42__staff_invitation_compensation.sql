-- Datos de nomina que el admin captura al invitar a un empleado; se conservan
-- hasta que el invitado confirma su registro y entonces se copian a
-- staff.staff_compensation.
CREATE TABLE IF NOT EXISTS staff.staff_invitation_compensation (
    invitation_id UUID PRIMARY KEY,
    clinic_id UUID NOT NULL,
    base_salary NUMERIC(14, 2) NOT NULL DEFAULT 0,
    pay_frequency VARCHAR(20) NOT NULL DEFAULT 'BIWEEKLY',
    payment_method VARCHAR(20) NOT NULL DEFAULT 'BANK_TRANSFER',
    payment_account_clabe VARCHAR(18),
    rfc VARCHAR(13),
    curp VARCHAR(18),
    nss VARCHAR(11),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_staff_invitation_compensation_base_salary CHECK (base_salary >= 0)
);
