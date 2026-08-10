package com.jclinical.agenda.domain.ports.out;

import java.util.UUID;

public interface PatientValidatorPort {
    boolean existsByIdAndClinicId(UUID patientId, UUID clinicId);
}
