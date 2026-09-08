package com.jclinical.staff.domain.ports.out;

import com.jclinical.staff.domain.model.StaffPermission;

import java.util.UUID;

/**
 * Resuelve si un usuario tiene concedido un permiso operativo dentro de una clinica.
 * Permite que los servicios de operaciones apliquen control de acceso sin depender
 * de la implementacion concreta del modelo de roles y overrides.
 */
public interface StaffPermissionCheckerPort {

    boolean hasPermission(UUID clinicId, UUID userId, StaffPermission permission);
}
