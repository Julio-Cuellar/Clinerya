package com.jclinical.records.domain.ports.out;

import com.jclinical.records.domain.model.MedicalHistory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MedicalHistoryRepositoryPort {
    Optional<MedicalHistory> findByPatientIdAndTemplateIdAndClinicId(UUID patientId, UUID templateId, UUID clinicId);
    List<MedicalHistory> findByPatientIdAndClinicId(UUID patientId, UUID clinicId);
    MedicalHistory save(MedicalHistory medicalHistory);
}
