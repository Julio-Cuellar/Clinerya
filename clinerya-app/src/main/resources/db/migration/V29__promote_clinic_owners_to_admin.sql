UPDATE staff.clinic_staff staff
SET role = 'CLINIC_ADMIN',
    updated_at = CURRENT_TIMESTAMP
FROM clinics.clinics clinic
WHERE clinic.id = staff.clinic_id
  AND clinic.owner_user_id = staff.user_id
  AND staff.role = 'DOCTOR';
