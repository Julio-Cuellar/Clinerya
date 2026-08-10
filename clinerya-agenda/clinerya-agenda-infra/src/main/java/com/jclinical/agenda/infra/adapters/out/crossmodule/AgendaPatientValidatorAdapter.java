package com.jclinical.agenda.infra.adapters.out.crossmodule;

import com.jclinical.agenda.domain.ports.out.PatientValidatorPort;
import com.jclinical.patients.domain.ports.in.GetPatientUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AgendaPatientValidatorAdapter implements PatientValidatorPort {

    private final GetPatientUseCase getPatientUseCase;

    @Override
    public boolean existsByIdAndClinicId(UUID patientId, UUID clinicId) {
        return getPatientUseCase.getPatientById(patientId)
                .map(patient -> patient.getClinicId().equals(clinicId))
                .orElse(false);
    }
}
