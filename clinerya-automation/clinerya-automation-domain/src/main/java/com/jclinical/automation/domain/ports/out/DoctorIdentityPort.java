package com.jclinical.automation.domain.ports.out;

import java.util.Optional;
import java.util.UUID;

/** Medico (miembro del personal que atiende pacientes) que corresponde a un usuario de Clinerya. */
public interface DoctorIdentityPort {

    Optional<UUID> doctorStaffIdOfUser(UUID clinicId, UUID userId);
}
