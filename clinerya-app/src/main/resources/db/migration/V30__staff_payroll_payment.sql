ALTER TABLE staff.staff_payroll_periods
    ADD COLUMN IF NOT EXISTS payment_status VARCHAR(20) NOT NULL DEFAULT 'UNPAID',
    ADD COLUMN IF NOT EXISTS paid_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS payment_account_id UUID,
    ADD COLUMN IF NOT EXISTS payment_journal_entry_id UUID;

UPDATE staff.staff_payroll_periods
SET payment_status = 'UNPAID'
WHERE payment_status IS NULL;

CREATE INDEX IF NOT EXISTS idx_staff_payroll_periods_payment_account
    ON staff.staff_payroll_periods (payment_account_id);
