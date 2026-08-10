package com.jclinical.patients.domain.ports.in;

import com.jclinical.patients.domain.model.Patient;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GetPatientUseCase {
    Optional<Patient> getPatientById(UUID id);
    List<Patient> getPatientsByClinic(UUID clinicId);
}
