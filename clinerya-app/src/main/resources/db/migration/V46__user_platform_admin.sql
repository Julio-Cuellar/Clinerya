-- Permiso de plataforma (superadmin), distinto de StaffPermission: esos son por clinica,
-- este es global al tenant. Necesario para /api/v1/system-configs, cuyo endpoint de
-- respaldo vuelca la base completa y hasta ahora era accesible por cualquier autenticado.
ALTER TABLE users.users
    ADD COLUMN platform_admin BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN users.users.platform_admin IS
    'Acceso a configuracion global y respaldos. Otorgar manualmente, nunca por auto-registro.';
