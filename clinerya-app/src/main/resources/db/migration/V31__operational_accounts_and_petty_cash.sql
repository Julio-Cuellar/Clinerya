ALTER TABLE accounting.bank_accounts
    ADD COLUMN IF NOT EXISTS account_kind VARCHAR(30) NOT NULL DEFAULT 'BANK';

UPDATE accounting.bank_accounts
SET account_kind = 'BANK'
WHERE account_kind IS NULL;

CREATE INDEX IF NOT EXISTS idx_accounting_bank_accounts_kind
    ON accounting.bank_accounts (clinic_id, account_kind, active);
