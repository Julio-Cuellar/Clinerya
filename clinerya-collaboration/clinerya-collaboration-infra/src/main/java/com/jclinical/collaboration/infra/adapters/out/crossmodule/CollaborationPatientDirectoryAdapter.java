package com.jclinical.collaboration.infra.adapters.out.crossmodule;

import com.jclinical.collaboration.domain.ports.out.PatientDirectoryPort;
import com.jclinical.patients.domain.model.Patient;
import com.jclinical.patients.domain.ports.in.GetPatientUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CollaborationPatientDirectoryAdapter implements PatientDirectoryPort {

    private final GetPatientUseCase getPatientUseCase;

    @Override
    public Optional<PatientSummary> findPatient(UUID patientId, UUID clinicId) {
        return getPatientUseCase.getPatientById(patientId)
                .filter(patient -> patient.getClinicId().equals(clinicId))
                .map(this::toSummary);
    }

    private PatientSummary toSummary(Patient patient) {
        String fullName = String.join(" ",
                nonBlank(patient.getFirstName()),
                nonBlank(patient.getLastNamePaterno()),
                nonBlank(patient.getLastNameMaterno())).trim().replaceAll(" +", " ");
        return new PatientSummary(patient.getId(), patient.getClinicId(), fullName);
    }

    private String nonBlank(String value) {
        return value == null ? "" : value;
    }
}
