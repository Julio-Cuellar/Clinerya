ALTER TABLE accounting.bank_accounts
    ADD COLUMN IF NOT EXISTS credit_cutoff_date date,
    ADD COLUMN IF NOT EXISTS credit_payment_due_date date,
    ADD COLUMN IF NOT EXISTS credit_limit numeric(38, 2),
    ADD COLUMN IF NOT EXISTS credit_current_amount numeric(38, 2),
    ADD COLUMN IF NOT EXISTS credit_minimum_payment numeric(38, 2),
    ADD COLUMN IF NOT EXISTS credit_no_interest_payment numeric(38, 2),
    ADD COLUMN IF NOT EXISTS credit_current_payment_due numeric(38, 2);
