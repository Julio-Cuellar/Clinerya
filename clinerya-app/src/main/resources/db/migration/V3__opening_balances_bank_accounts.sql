CREATE TABLE IF NOT EXISTS accounting.opening_balance_setups (
    cash_opening_amount numeric(38,2) NOT NULL,
    inventory_opening_amount numeric(38,2) NOT NULL,
    total_opening_assets numeric(38,2) NOT NULL,
    entry_date date NOT NULL,
    created_at timestamp(6) NOT NULL,
    clinic_id uuid NOT NULL UNIQUE,
    id uuid NOT NULL,
    journal_entry_id uuid NOT NULL UNIQUE,
    capital_account_code varchar(50) NOT NULL,
    capital_account_name varchar(255) NOT NULL,
    notes TEXT,
    PRIMARY KEY (id),
    CONSTRAINT FK_opening_balance_journal_entry
        FOREIGN KEY (journal_entry_id) REFERENCES accounting.journal_entries(id),
    CONSTRAINT CK_opening_balance_cash_non_negative
        CHECK (cash_opening_amount >= 0),
    CONSTRAINT CK_opening_balance_inventory_non_negative
        CHECK (inventory_opening_amount >= 0),
    CONSTRAINT CK_opening_balance_total_positive
        CHECK (total_opening_assets > 0)
);

CREATE TABLE IF NOT EXISTS accounting.bank_accounts (
    active boolean NOT NULL,
    opening_balance numeric(38,2) NOT NULL,
    opening_date date NOT NULL,
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    clinic_id uuid NOT NULL,
    id uuid NOT NULL,
    opening_balance_setup_id uuid,
    opening_journal_entry_id uuid,
    account_last4 varchar(4),
    currency varchar(3) NOT NULL,
    account_code varchar(50) NOT NULL,
    alias varchar(120) NOT NULL,
    bank_name varchar(120) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT FK_bank_account_opening_balance_setup
        FOREIGN KEY (opening_balance_setup_id) REFERENCES accounting.opening_balance_setups(id),
    CONSTRAINT FK_bank_account_opening_journal_entry
        FOREIGN KEY (opening_journal_entry_id) REFERENCES accounting.journal_entries(id),
    CONSTRAINT CK_bank_account_opening_balance_non_negative
        CHECK (opening_balance >= 0),
    CONSTRAINT CK_bank_account_currency_length
        CHECK (char_length(currency) = 3),
    CONSTRAINT CK_bank_account_last4_digits
        CHECK (account_last4 IS NULL OR account_last4 ~ '^[0-9]{4}$')
);

CREATE INDEX IF NOT EXISTS IDX_bank_accounts_clinic_id
    ON accounting.bank_accounts (clinic_id);

CREATE INDEX IF NOT EXISTS IDX_bank_accounts_opening_balance_setup_id
    ON accounting.bank_accounts (opening_balance_setup_id);

CREATE INDEX IF NOT EXISTS IDX_opening_balance_setups_clinic_id
    ON accounting.opening_balance_setups (clinic_id);
