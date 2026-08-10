package com.jclinical.records.domain.ports.out;

import java.util.Optional;
import java.util.UUID;

public interface PatientLookupPort {
    Optional<PatientDetails> findPatient(UUID patientId);

    record PatientDetails(
            UUID id,
            UUID clinicId,
            String fullName,
            String curp,
            String phone,
            String email
    ) {}
}
