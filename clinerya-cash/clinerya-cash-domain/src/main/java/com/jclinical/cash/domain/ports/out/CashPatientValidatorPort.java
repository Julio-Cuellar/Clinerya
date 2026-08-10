package com.jclinical.cash.domain.ports.out;

import java.util.UUID;

public interface CashPatientValidatorPort {
    boolean existsByIdAndClinicId(UUID patientId, UUID clinicId);
}
