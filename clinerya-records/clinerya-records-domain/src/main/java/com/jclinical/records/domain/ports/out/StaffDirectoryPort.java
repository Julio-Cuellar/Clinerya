package com.jclinical.records.domain.ports.out;

import java.util.Optional;
import java.util.UUID;

/**
 * Resuelve nombres legibles del personal de una clínica para mostrarlos en el
 * expediente (autor de una nota, firmante) en vez de UUIDs crudos.
 */
public interface StaffDirectoryPort {

    /** Nombre del miembro del staff por su id de staff. */
    Optional<String> staffName(UUID staffId, UUID clinicId);

    /** Nombre del miembro del staff activo asociado a un id de usuario. */
    Optional<String> userName(UUID userId, UUID clinicId);
}
