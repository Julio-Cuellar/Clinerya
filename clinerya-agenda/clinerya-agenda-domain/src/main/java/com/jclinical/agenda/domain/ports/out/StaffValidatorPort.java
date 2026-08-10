package com.jclinical.agenda.domain.ports.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StaffValidatorPort {

    Optional<DoctorSnapshot> findActiveDoctor(UUID staffId, UUID clinicId);

    List<DoctorSnapshot> listActiveDoctors(UUID clinicId);

    record DoctorSnapshot(
            UUID staffId,
            String fullName
    ) {}
}
