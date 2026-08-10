package com.jclinical.cash.infra.adapters.out.crossmodule;

import com.jclinical.cash.domain.ports.out.CashPatientValidatorPort;
import com.jclinical.patients.domain.ports.in.GetPatientUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CashPatientValidatorAdapter implements CashPatientValidatorPort {

    private final GetPatientUseCase getPatientUseCase;

    @Override
    public boolean existsByIdAndClinicId(UUID patientId, UUID clinicId) {
        return getPatientUseCase.getPatientById(patientId)
                .map(patient -> patient.getClinicId().equals(clinicId))
                .orElse(false);
    }
}
