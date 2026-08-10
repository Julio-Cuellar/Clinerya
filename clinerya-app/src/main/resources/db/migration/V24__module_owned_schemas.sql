CREATE SCHEMA IF NOT EXISTS app;
CREATE SCHEMA IF NOT EXISTS users;
CREATE SCHEMA IF NOT EXISTS clinics;
CREATE SCHEMA IF NOT EXISTS staff;
CREATE SCHEMA IF NOT EXISTS agenda;
CREATE SCHEMA IF NOT EXISTS patients;
CREATE SCHEMA IF NOT EXISTS records;
CREATE SCHEMA IF NOT EXISTS attachments;
CREATE SCHEMA IF NOT EXISTS collaboration;
CREATE SCHEMA IF NOT EXISTS inventory;
CREATE SCHEMA IF NOT EXISTS treatments;
CREATE SCHEMA IF NOT EXISTS cash;
CREATE SCHEMA IF NOT EXISTS integrations;

CREATE OR REPLACE FUNCTION core.move_table_to_schema(table_name text, target_schema text)
RETURNS void
LANGUAGE plpgsql
AS $$
BEGIN
    IF to_regclass(format('core.%I', table_name)) IS NOT NULL
        AND to_regclass(format('%I.%I', target_schema, table_name)) IS NULL THEN
        EXECUTE format('ALTER TABLE core.%I SET SCHEMA %I', table_name, target_schema);
    END IF;
END;
$$;

SELECT core.move_table_to_schema('system_configs', 'app');

SELECT core.move_table_to_schema('users', 'users');
SELECT core.move_table_to_schema('user_pre_registrations', 'users');

SELECT core.move_table_to_schema('clinics', 'clinics');
SELECT core.move_table_to_schema('clinic_rooms', 'clinics');

SELECT core.move_table_to_schema('clinic_staff', 'staff');
SELECT core.move_table_to_schema('clinic_staff_invitation', 'staff');
SELECT core.move_table_to_schema('doctor_profiles', 'staff');

SELECT core.move_table_to_schema('clinic_schedules', 'agenda');
SELECT core.move_table_to_schema('appointments', 'agenda');
SELECT core.move_table_to_schema('waiting_list', 'agenda');

SELECT core.move_table_to_schema('patients', 'patients');

SELECT core.move_table_to_schema('clinical_notes', 'records');
SELECT core.move_table_to_schema('medical_history_templates', 'records');
SELECT core.move_table_to_schema('medical_histories', 'records');
SELECT core.move_table_to_schema('medical_history_versions', 'records');
SELECT core.move_table_to_schema('document_signatures', 'records');
SELECT core.move_table_to_schema('privacy_consents', 'records');
SELECT core.move_table_to_schema('record_access_logs', 'records');
SELECT core.move_table_to_schema('record_access_log_outbox', 'records');
SELECT core.move_table_to_schema('temporary_record_shares', 'records');
SELECT core.move_table_to_schema('prescriptions', 'records');
SELECT core.move_table_to_schema('prescription_items', 'records');

SELECT core.move_table_to_schema('attachments', 'attachments');

SELECT core.move_table_to_schema('external_access_grants', 'collaboration');

SELECT core.move_table_to_schema('materials', 'inventory');
SELECT core.move_table_to_schema('inventory_batches', 'inventory');
SELECT core.move_table_to_schema('inventory_movements', 'inventory');
SELECT core.move_table_to_schema('material_reservations', 'inventory');
SELECT core.move_table_to_schema('inventory_suppliers', 'inventory');
SELECT core.move_table_to_schema('supplier_materials', 'inventory');
SELECT core.move_table_to_schema('purchase_orders', 'inventory');
SELECT core.move_table_to_schema('purchase_order_lines', 'inventory');
SELECT core.move_table_to_schema('purchase_receipts', 'inventory');
SELECT core.move_table_to_schema('purchase_receipt_lines', 'inventory');

SELECT core.move_table_to_schema('treatment_catalog_items', 'treatments');
SELECT core.move_table_to_schema('treatment_catalog_materials', 'treatments');
SELECT core.move_table_to_schema('quotations', 'treatments');
SELECT core.move_table_to_schema('quotation_items', 'treatments');
SELECT core.move_table_to_schema('quotation_item_materials', 'treatments');
SELECT core.move_table_to_schema('visits', 'treatments');
SELECT core.move_table_to_schema('visit_line_items', 'treatments');
SELECT core.move_table_to_schema('visit_material_usages', 'treatments');

SELECT core.move_table_to_schema('cash_sessions', 'cash');
SELECT core.move_table_to_schema('cash_tickets', 'cash');
SELECT core.move_table_to_schema('cash_payment_lines', 'cash');
SELECT core.move_table_to_schema('cash_expenses', 'cash');
SELECT core.move_table_to_schema('cash_folio_counters', 'cash');

SELECT core.move_table_to_schema('staff_calendar_credentials', 'integrations');
SELECT core.move_table_to_schema('external_calendar_events', 'integrations');

DROP FUNCTION core.move_table_to_schema(text, text);
