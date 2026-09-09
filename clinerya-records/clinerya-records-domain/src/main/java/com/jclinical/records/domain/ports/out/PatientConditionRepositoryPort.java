package com.jclinical.records.domain.ports.out;

import com.jclinical.records.domain.model.ClinicalDataSource;
import com.jclinical.records.domain.model.PatientCondition;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PatientConditionRepositoryPort {
    PatientCondition save(PatientCondition condition);
    List<PatientCondition> findByClinicIdAndPatientId(UUID clinicId, UUID patientId);
    Optional<PatientCondition> findByIdAndClinicId(UUID id, UUID clinicId);
    void deleteByIdAndClinicId(UUID id, UUID clinicId);
    void deleteByClinicIdAndPatientIdAndSource(UUID clinicId, UUID patientId, ClinicalDataSource source);
}
