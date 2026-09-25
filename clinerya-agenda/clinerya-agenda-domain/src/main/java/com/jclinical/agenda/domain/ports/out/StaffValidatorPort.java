package com.jclinical.agenda.domain.ports.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StaffValidatorPort {

    Optional<DoctorSnapshot> findActiveDoctor(UUID staffId, UUID clinicId);

    List<DoctorSnapshot> listActiveDoctors(UUID clinicId);

    /** Usuario de Clinerya del miembro del personal, para saber si quien llama es ese medico. */
    default Optional<UUID> userIdOfStaff(UUID staffId, UUID clinicId) {
        return Optional.empty();
    }

    record DoctorSnapshot(
            UUID staffId,
            String fullName
    ) {}
}
