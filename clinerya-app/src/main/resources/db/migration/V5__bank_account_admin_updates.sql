ALTER TABLE accounting.bank_accounts
    ADD COLUMN IF NOT EXISTS last_modified_at timestamp(6),
    ADD COLUMN IF NOT EXISTS last_modification_reason TEXT;
