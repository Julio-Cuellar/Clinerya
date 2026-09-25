-- Un administrador de clinica puede ademas atender pacientes. V29 paso a los duenos de DOCTOR a
-- CLINIC_ADMIN y con eso dejaron de aparecer en la agenda, pero conservaron el acceso clinico
-- porque los roles administrativos reciben todos los permisos. Esta bandera separa ambas cosas:
-- solo un administrador que atiende pacientes cuenta como doctor (agenda, consultorios y
-- expediente). Para un DOCTOR la bandera no se consulta: atiende pacientes por su rol.
--
-- Atender pacientes exige cedula profesional, asi que solo se migran como tales los
-- administradores cuyo perfil medico ya la tiene capturada. Los demas pierden el acceso clinico
-- hasta registrarla desde Personal.
ALTER TABLE staff.clinic_staff
    ADD COLUMN IF NOT EXISTS attends_patients boolean NOT NULL DEFAULT false;

UPDATE staff.clinic_staff staff
SET attends_patients = true,
    updated_at = CURRENT_TIMESTAMP
FROM staff.doctor_profiles profile
WHERE profile.clinic_staff_id = staff.id
  AND staff.role IN ('ADMIN', 'CLINIC_ADMIN')
  AND profile.cedula_profesional IS NOT NULL
  AND btrim(profile.cedula_profesional) <> '';
