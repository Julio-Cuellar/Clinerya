package com.jclinical.records.infra.adapters.out.persistence;

import com.jclinical.patients.domain.ports.in.GetPatientUseCase;
import com.jclinical.records.domain.ports.out.PatientLookupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PatientLookupAdapter implements PatientLookupPort {

    private final GetPatientUseCase getPatientUseCase;

    @Override
    public Optional<PatientDetails> findPatient(UUID patientId) {
        return getPatientUseCase.getPatientById(patientId)
                .map(patient -> {
                    String fullName = patient.getFirstName() + " " +
                            (patient.getLastNamePaterno() != null ? patient.getLastNamePaterno() : "") + " " +
                            (patient.getLastNameMaterno() != null ? patient.getLastNameMaterno() : "");
                    fullName = fullName.replaceAll("\\s+", " ").trim();
                    return new PatientDetails(
                            patient.getId(),
                            patient.getClinicId(),
                            fullName,
                            patient.getCurp(),
                            patient.getPhone(),
                            patient.getEmail(),
                            patient.getBloodType() != null ? patient.getBloodType().name() : null
                    );
                });
    }
}
