package com.jclinical.records.infra.adapters.out.persistence;

import com.jclinical.patients.domain.ports.in.GetPatientUseCase;
import com.jclinical.records.domain.ports.out.PatientValidatorPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PatientValidatorAdapter implements PatientValidatorPort {

    private final GetPatientUseCase getPatientUseCase;

    @Override
    public boolean existsByIdAndClinicId(UUID patientId, UUID clinicId) {
        return getPatientUseCase.getPatientById(patientId)
                .map(patient -> patient.getClinicId().equals(clinicId))
                .orElse(false);
    }
}
