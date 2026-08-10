package com.jclinical.patients.domain.ports.out;

import com.jclinical.patients.domain.model.Patient;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PatientRepositoryPort {
    Patient save(Patient patient);
    Optional<Patient> findById(UUID id);
    List<Patient> findByClinicId(UUID clinicId);
    boolean existsByCurpAndClinicId(String curp, UUID clinicId);
    boolean existsById(UUID id);
    void deleteById(UUID id);
}
