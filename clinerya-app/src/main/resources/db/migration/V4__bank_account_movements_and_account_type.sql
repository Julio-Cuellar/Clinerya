ALTER TABLE accounting.bank_accounts
    ADD COLUMN IF NOT EXISTS account_type varchar(20) NOT NULL DEFAULT 'DEBIT',
    ADD COLUMN IF NOT EXISTS deactivated_at timestamp(6),
    ADD COLUMN IF NOT EXISTS deactivation_reason TEXT;

ALTER TABLE accounting.bank_accounts
    DROP CONSTRAINT IF EXISTS CK_bank_accounts_account_type;

ALTER TABLE accounting.bank_accounts
    ADD CONSTRAINT CK_bank_accounts_account_type
        CHECK (account_type IN ('DEBIT', 'CREDIT'));

ALTER TABLE accounting.journal_lines
    ADD COLUMN IF NOT EXISTS bank_account_id uuid;

CREATE INDEX IF NOT EXISTS IDX_journal_lines_bank_account_id
    ON accounting.journal_lines (bank_account_id);

CREATE INDEX IF NOT EXISTS IDX_bank_accounts_active
    ON accounting.bank_accounts (clinic_id, active);
