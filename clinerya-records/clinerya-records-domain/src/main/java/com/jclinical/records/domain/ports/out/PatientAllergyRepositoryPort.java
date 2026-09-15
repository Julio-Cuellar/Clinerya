package com.jclinical.records.domain.ports.out;

import com.jclinical.records.domain.model.ClinicalDataSource;
import com.jclinical.records.domain.model.PatientAllergy;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PatientAllergyRepositoryPort {
    PatientAllergy save(PatientAllergy allergy);
    List<PatientAllergy> findByClinicIdAndPatientId(UUID clinicId, UUID patientId);
    Optional<PatientAllergy> findByIdAndClinicId(UUID id, UUID clinicId);
    void deleteByIdAndClinicId(UUID id, UUID clinicId);
    void deleteByClinicIdAndPatientIdAndSource(UUID clinicId, UUID patientId, ClinicalDataSource source);
}
