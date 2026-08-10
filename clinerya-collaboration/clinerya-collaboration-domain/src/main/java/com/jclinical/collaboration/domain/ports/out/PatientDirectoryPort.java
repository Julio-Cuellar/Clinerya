package com.jclinical.collaboration.domain.ports.out;

import java.util.Optional;
import java.util.UUID;

public interface PatientDirectoryPort {

    Optional<PatientSummary> findPatient(UUID patientId, UUID clinicId);

    record PatientSummary(UUID id, UUID clinicId, String fullName) {}
}
