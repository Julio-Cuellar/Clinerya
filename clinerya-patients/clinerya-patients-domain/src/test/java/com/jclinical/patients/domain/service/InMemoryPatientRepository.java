package com.jclinical.patients.domain.service;

import com.jclinical.patients.domain.model.Patient;
import com.jclinical.patients.domain.ports.out.PatientRepositoryPort;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

class InMemoryPatientRepository implements PatientRepositoryPort {

    final Map<UUID, Patient> patients = new LinkedHashMap<>();

    @Override
    public Patient save(Patient patient) {
        patients.put(patient.getId(), patient);
        return patient;
    }

    @Override
    public Optional<Patient> findById(UUID id) {
        return Optional.ofNullable(patients.get(id));
    }

    @Override
    public List<Patient> findByClinicId(UUID clinicId) {
        return patients.values().stream().filter(patient -> clinicId.equals(patient.getClinicId())).toList();
    }

    @Override
    public boolean existsByCurpAndClinicId(String curp, UUID clinicId) {
        return patients.values().stream()
                .anyMatch(patient -> curp.equals(patient.getCurp()) && clinicId.equals(patient.getClinicId()));
    }

    @Override
    public boolean existsById(UUID id) {
        return patients.containsKey(id);
    }

    @Override
    public void deleteById(UUID id) {
        patients.remove(id);
    }
}
