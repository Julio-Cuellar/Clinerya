-- ============================================================
-- V1 - MediCloud Initial Schema
-- Generado a partir de las entidades JPA del proyecto.
-- Incluye todos los esquemas y tablas del dominio.
-- ============================================================

-- ============================================================
-- Schemas
-- ============================================================
CREATE SCHEMA IF NOT EXISTS accounting;
CREATE SCHEMA IF NOT EXISTS core;

-- ============================================================
-- accounting schema
-- ============================================================

CREATE TABLE IF NOT EXISTS accounting.journal_entries (
    entry_date date NOT NULL,
    created_at timestamp(6) NOT NULL,
    clinic_id uuid NOT NULL,
    id uuid NOT NULL,
    source_event_id uuid UNIQUE,
    description TEXT,
    source_event_type varchar(255),
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS accounting.journal_lines (
    credit numeric(19,4) NOT NULL,
    debit numeric(19,4) NOT NULL,
    id uuid NOT NULL,
    journal_entry_id uuid NOT NULL,
    account_code varchar(255) NOT NULL,
    account_name varchar(255) NOT NULL,
    PRIMARY KEY (id)
);

-- ============================================================
-- core schema - auth & users
-- ============================================================

CREATE TABLE IF NOT EXISTS core.users (
    email_verified boolean NOT NULL,
    failed_login_attempts integer NOT NULL,
    is_active boolean NOT NULL,
    created_at timestamp(6) NOT NULL,
    last_login_at timestamp(6),
    locked_until timestamp(6),
    updated_at timestamp(6) NOT NULL,
    verification_token_expires_at timestamp(6),
    id uuid NOT NULL,
    avatar_url varchar(255),
    email varchar(255) NOT NULL UNIQUE,
    full_name varchar(255) NOT NULL,
    password_hash varchar(255) NOT NULL,
    phone varchar(255),
    theme_preference varchar(255) NOT NULL,
    verification_token varchar(255),
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS core.user_pre_registrations (
    created_at timestamp(6) NOT NULL,
    verification_token_expires_at timestamp(6) NOT NULL,
    id uuid NOT NULL,
    clinic_name varchar(255) NOT NULL,
    email varchar(255) NOT NULL UNIQUE,
    full_name varchar(255) NOT NULL,
    password_hash varchar(255) NOT NULL,
    verification_token varchar(255) NOT NULL,
    PRIMARY KEY (id)
);

-- ============================================================
-- core schema - clinics & staff
-- ============================================================

CREATE TABLE IF NOT EXISTS core.clinics (
    is_active boolean NOT NULL,
    material_reservation_lead_days integer,
    created_at timestamp(6) NOT NULL,
    data_processor_agreed_at timestamp(6),
    updated_at timestamp(6) NOT NULL,
    id uuid NOT NULL,
    legal_representative_staff_id uuid,
    organization_id uuid,
    owner_user_id uuid NOT NULL,
    address_colonia varchar(255),
    address_municipality varchar(255),
    address_state varchar(255),
    address_street varchar(255),
    address_zip varchar(255),
    email varchar(255),
    legal_name varchar(255),
    logo_url varchar(255),
    name varchar(255) NOT NULL,
    phone varchar(255),
    privacy_notice_url varchar(255),
    rfc varchar(255),
    tax_regime_code varchar(255),
    timezone varchar(255) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS core.clinic_staff (
    end_date date,
    hire_date date,
    is_active boolean NOT NULL,
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    clinic_id uuid NOT NULL,
    id uuid NOT NULL,
    user_id uuid NOT NULL,
    employee_code varchar(255),
    notes varchar(255),
    role varchar(255) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS core.clinic_staff_invitation (
    id uuid NOT NULL,
    clinic_id uuid NOT NULL,
    email varchar(255) NOT NULL,
    role varchar(255) NOT NULL,
    token varchar(255) NOT NULL UNIQUE,
    used boolean NOT NULL,
    created_at timestamp(6) NOT NULL,
    expires_at timestamp(6) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS core.clinic_schedules (
    end_time time(6),
    open boolean NOT NULL,
    start_time time(6),
    clinic_id uuid NOT NULL,
    id uuid NOT NULL,
    day_of_week varchar(255) NOT NULL CHECK (day_of_week IN ('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY')),
    PRIMARY KEY (id),
    UNIQUE (clinic_id, day_of_week)
);

CREATE TABLE IF NOT EXISTS core.doctor_profiles (
    anio_egreso integer,
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    verified_at timestamp(6),
    clinic_id uuid NOT NULL,
    clinic_staff_id uuid NOT NULL UNIQUE,
    id uuid NOT NULL,
    verified_by_user_id uuid,
    cedula_especialidad varchar(255),
    cedula_profesional varchar(255),
    credential_status varchar(255) NOT NULL,
    especialidad varchar(255),
    sub_especialidad varchar(255),
    universidad_egreso varchar(255),
    PRIMARY KEY (id)
);

-- ============================================================
-- core schema - patients & records
-- ============================================================

CREATE TABLE IF NOT EXISTS core.patients (
    date_of_birth date NOT NULL,
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    clinic_id uuid NOT NULL,
    id uuid NOT NULL,
    address_colonia varchar(255),
    address_indoor_number varchar(255),
    address_municipality varchar(255),
    address_outdoor_number varchar(255),
    address_state varchar(255),
    address_street varchar(255),
    address_zip_code varchar(255),
    blood_type varchar(255),
    curp varchar(255),
    email varchar(255),
    emergency_contact_full_name varchar(255),
    emergency_contact_phone varchar(255),
    emergency_contact_relationship varchar(255),
    first_name varchar(255) NOT NULL,
    gender varchar(255) NOT NULL,
    last_name_materno varchar(255),
    last_name_paterno varchar(255) NOT NULL,
    marital_status varchar(255),
    nationality varchar(255),
    occupation varchar(255),
    phone varchar(255) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (clinic_id, curp)
);

CREATE TABLE IF NOT EXISTS core.clinical_notes (
    vital_bmi float(53),
    vital_height float(53),
    vital_hr integer,
    vital_o2 integer,
    vital_rr integer,
    vital_temp float(53),
    vital_weight float(53),
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    authored_by_external_user_id uuid,
    clinic_id uuid NOT NULL,
    doctor_id uuid NOT NULL,
    id uuid NOT NULL,
    patient_id uuid NOT NULL,
    assessment TEXT,
    objective TEXT,
    plan TEXT,
    status varchar(255) NOT NULL,
    subjective TEXT,
    vital_bp varchar(255),
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS core.medical_history_templates (
    active boolean NOT NULL,
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    clinic_id uuid NOT NULL,
    id uuid NOT NULL,
    description TEXT,
    name varchar(255) NOT NULL,
    schema_json TEXT,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS core.medical_histories (
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    clinic_id uuid NOT NULL,
    id uuid NOT NULL,
    patient_id uuid NOT NULL,
    template_id uuid NOT NULL,
    answers_json TEXT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (patient_id, template_id, clinic_id)
);

CREATE TABLE IF NOT EXISTS core.attachments (
    created_at timestamp(6) NOT NULL,
    size_bytes bigint NOT NULL,
    clinic_id uuid NOT NULL,
    id uuid NOT NULL,
    patient_id uuid NOT NULL,
    content_type varchar(255) NOT NULL,
    element_id varchar(255) NOT NULL,
    original_filename varchar(255) NOT NULL,
    stored_path varchar(255) NOT NULL,
    PRIMARY KEY (id)
);

-- ============================================================
-- core schema - collaboration (shared access)
-- ============================================================

CREATE TABLE IF NOT EXISTS core.external_access_grants (
    created_at timestamp(6) NOT NULL,
    responded_at timestamp(6),
    revoked_at timestamp(6),
    external_user_id uuid,
    id uuid NOT NULL,
    invited_by_staff_id uuid NOT NULL,
    patient_id uuid NOT NULL,
    source_clinic_id uuid NOT NULL,
    access_level varchar(255) NOT NULL,
    invited_email varchar(255) NOT NULL,
    status varchar(255) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS core.temporary_record_shares (
    id uuid NOT NULL,
    clinic_id uuid NOT NULL,
    patient_id uuid NOT NULL,
    email varchar(255) NOT NULL,
    token varchar(255) NOT NULL UNIQUE,
    expires_at timestamp(6) NOT NULL,
    created_at timestamp(6) NOT NULL,
    PRIMARY KEY (id)
);

-- ============================================================
-- core schema - inventory
-- ============================================================

CREATE TABLE IF NOT EXISTS core.materials (
    active boolean NOT NULL,
    current_stock numeric(19,4) NOT NULL,
    minimum_stock numeric(19,4),
    quantity_per_presentation numeric(19,4),
    reserved_quantity numeric(19,4),
    sale_enabled boolean NOT NULL,
    sale_price numeric(19,4),
    tracks_batches boolean NOT NULL,
    unit_cost numeric(19,4) NOT NULL,
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    version bigint,
    clinic_id uuid NOT NULL,
    id uuid NOT NULL,
    brand varchar(255),
    category varchar(255),
    description TEXT,
    internal_code varchar(255),
    name varchar(255) NOT NULL,
    presentation_name varchar(255),
    unit_of_measure varchar(255) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS core.inventory_batches (
    expiration_date date,
    initial_quantity numeric(19,4) NOT NULL,
    remaining_quantity numeric(19,4) NOT NULL,
    unit_cost_at_entry numeric(19,4) NOT NULL,
    created_at timestamp(6) NOT NULL,
    clinic_id uuid NOT NULL,
    id uuid NOT NULL,
    material_id uuid NOT NULL,
    lot_number varchar(255),
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS core.inventory_movements (
    presentation_quantity numeric(19,4),
    quantity numeric(19,4) NOT NULL,
    quantity_per_presentation_at_movement numeric(19,4),
    unit_cost_at_movement numeric(19,4) NOT NULL,
    created_at timestamp(6) NOT NULL,
    movement_date timestamp(6) NOT NULL,
    batch_id uuid,
    clinic_id uuid NOT NULL,
    id uuid NOT NULL,
    material_id uuid NOT NULL,
    reference_id uuid,
    notes TEXT,
    presentation_name_at_movement varchar(255),
    reference_type varchar(255),
    type varchar(255) NOT NULL CHECK (type IN ('PURCHASE_ENTRY','USAGE_EXIT','SALE_EXIT','ADJUSTMENT_IN','ADJUSTMENT_OUT')),
    PRIMARY KEY (id)
);

-- ============================================================
-- core schema - treatments & quotations
-- ============================================================

CREATE TABLE IF NOT EXISTS core.treatment_catalog_items (
    active boolean NOT NULL,
    default_price numeric(38,2) NOT NULL,
    estimated_duration_minutes integer,
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    clinic_id uuid NOT NULL,
    id uuid NOT NULL,
    category varchar(255),
    description TEXT,
    name varchar(255) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS core.treatment_catalog_materials (
    typical_quantity numeric(19,4),
    catalog_item_id uuid NOT NULL,
    id uuid NOT NULL,
    material_id uuid NOT NULL,
    material_name varchar(255) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS core.quotations (
    quotation_date date NOT NULL,
    valid_until date,
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    clinic_id uuid NOT NULL,
    created_by_user_id uuid NOT NULL,
    id uuid NOT NULL,
    patient_id uuid NOT NULL,
    notes TEXT,
    status varchar(255) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS core.quotation_items (
    discount_percentage numeric(38,2),
    labor_charge numeric(19,4) NOT NULL,
    tooth_number integer,
    catalog_item_id uuid,
    id uuid NOT NULL,
    quotation_id uuid NOT NULL,
    description TEXT NOT NULL,
    progress_status varchar(255) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS core.quotation_item_materials (
    estimated_quantity numeric(19,4) NOT NULL,
    unit_cost_at_quote numeric(19,4) NOT NULL,
    id uuid NOT NULL,
    material_id uuid,
    quotation_item_id uuid NOT NULL,
    material_name varchar(255) NOT NULL,
    PRIMARY KEY (id)
);

-- ============================================================
-- core schema - agenda & appointments
-- ============================================================

CREATE TABLE IF NOT EXISTS core.appointments (
    materials_reserved boolean NOT NULL,
    created_at timestamp(6) NOT NULL,
    scheduled_end timestamp(6) NOT NULL,
    scheduled_start timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    clinic_id uuid NOT NULL,
    doctor_staff_id uuid NOT NULL,
    id uuid NOT NULL,
    patient_id uuid NOT NULL,
    quotation_id uuid,
    quotation_item_id uuid,
    notes TEXT,
    reason varchar(255),
    status varchar(255) NOT NULL CHECK (status IN ('SCHEDULED','CONFIRMED','COMPLETED','CANCELLED','NO_SHOW')),
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS core.material_reservations (
    quantity numeric(19,4) NOT NULL,
    created_at timestamp(6) NOT NULL,
    released_at timestamp(6),
    appointment_id uuid NOT NULL,
    clinic_id uuid NOT NULL,
    id uuid NOT NULL,
    material_id uuid NOT NULL,
    material_name varchar(255),
    status varchar(255) NOT NULL CHECK (status IN ('RESERVED','RELEASED')),
    PRIMARY KEY (id)
);

-- ============================================================
-- core schema - cash & visits
-- ============================================================

CREATE TABLE IF NOT EXISTS core.cash_sessions (
    cash_difference numeric(38,2),
    counted_cash_amount numeric(38,2),
    expected_cash_amount numeric(38,2),
    opening_amount numeric(38,2) NOT NULL,
    closed_at timestamp(6),
    opened_at timestamp(6) NOT NULL,
    clinic_id uuid NOT NULL,
    closed_by_staff_id uuid,
    id uuid NOT NULL,
    opened_by_staff_id uuid NOT NULL,
    status varchar(255) NOT NULL CHECK (status IN ('OPEN','CLOSED')),
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS core.cash_tickets (
    discount_amount numeric(38,2),
    folio integer NOT NULL,
    total_amount numeric(38,2) NOT NULL,
    created_at timestamp(6) NOT NULL,
    voided_at timestamp(6),
    cash_session_id uuid NOT NULL,
    clinic_id uuid NOT NULL,
    created_by_staff_id uuid NOT NULL,
    discount_authorized_by_staff_id uuid,
    id uuid NOT NULL,
    patient_id uuid NOT NULL,
    quotation_id uuid,
    voided_by_staff_id uuid,
    concept varchar(255),
    discount_reason varchar(255),
    status varchar(255) NOT NULL CHECK (status IN ('ACTIVE','VOIDED')),
    void_reason varchar(255),
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS core.cash_payment_lines (
    amount numeric(38,2) NOT NULL,
    id uuid NOT NULL,
    ticket_id uuid NOT NULL,
    method varchar(255) NOT NULL CHECK (method IN ('CASH','TRANSFER','CARD','CHECK')),
    reference varchar(255),
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS core.cash_expenses (
    amount numeric(38,2) NOT NULL,
    created_at timestamp(6) NOT NULL,
    voided_at timestamp(6),
    cash_session_id uuid NOT NULL,
    clinic_id uuid NOT NULL,
    created_by_staff_id uuid NOT NULL,
    id uuid NOT NULL,
    voided_by_staff_id uuid,
    concept varchar(255) NOT NULL,
    status varchar(255) NOT NULL CHECK (status IN ('ACTIVE','VOIDED')),
    void_reason varchar(255),
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS core.cash_folio_counters (
    last_folio integer NOT NULL,
    clinic_id uuid NOT NULL,
    PRIMARY KEY (clinic_id)
);

CREATE TABLE IF NOT EXISTS core.visits (
    visit_date date NOT NULL,
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    clinic_id uuid NOT NULL,
    doctor_id uuid,
    id uuid NOT NULL,
    patient_id uuid NOT NULL,
    quotation_id uuid,
    notes TEXT,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS core.visit_line_items (
    id uuid NOT NULL,
    quotation_item_id uuid,
    visit_id uuid NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS core.visit_material_usages (
    actual_quantity numeric(19,4) NOT NULL,
    id uuid NOT NULL,
    material_id uuid,
    visit_line_item_id uuid NOT NULL,
    material_name varchar(255) NOT NULL,
    PRIMARY KEY (id)
);

-- ============================================================
-- Foreign Key Constraints
-- ============================================================

ALTER TABLE IF EXISTS accounting.journal_lines
    ADD CONSTRAINT FK_journal_lines_entry
    FOREIGN KEY (journal_entry_id) REFERENCES accounting.journal_entries;

ALTER TABLE IF EXISTS core.cash_payment_lines
    ADD CONSTRAINT FK_cash_payment_lines_ticket
    FOREIGN KEY (ticket_id) REFERENCES core.cash_tickets;

ALTER TABLE IF EXISTS core.quotation_item_materials
    ADD CONSTRAINT FK_quotation_item_materials_item
    FOREIGN KEY (quotation_item_id) REFERENCES core.quotation_items;

ALTER TABLE IF EXISTS core.quotation_items
    ADD CONSTRAINT FK_quotation_items_quotation
    FOREIGN KEY (quotation_id) REFERENCES core.quotations;

ALTER TABLE IF EXISTS core.treatment_catalog_materials
    ADD CONSTRAINT FK_treatment_catalog_materials_item
    FOREIGN KEY (catalog_item_id) REFERENCES core.treatment_catalog_items;

ALTER TABLE IF EXISTS core.visit_line_items
    ADD CONSTRAINT FK_visit_line_items_visit
    FOREIGN KEY (visit_id) REFERENCES core.visits;

ALTER TABLE IF EXISTS core.visit_material_usages
    ADD CONSTRAINT FK_visit_material_usages_line_item
    FOREIGN KEY (visit_line_item_id) REFERENCES core.visit_line_items;
