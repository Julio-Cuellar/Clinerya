-- ClinicAccessInterceptor y ClinicStaffService.requirePermission consultan esta combinación
-- en practicamente cada request a /api/v1/clinics/**. Sin índice, es un sequential scan
-- completo de staff.clinic_staff en cada llamada.
-- (clinic_staff vive en el esquema "staff", no "core", desde V24__module_owned_schemas.sql)
CREATE INDEX IF NOT EXISTS idx_clinic_staff_clinic_id_user_id
    ON staff.clinic_staff (clinic_id, user_id);
