ALTER TABLE core.cash_payment_lines
    ADD COLUMN IF NOT EXISTS bank_account_id uuid;

CREATE INDEX IF NOT EXISTS IDX_cash_payment_lines_bank_account_id
    ON core.cash_payment_lines (bank_account_id);
