package com.jclinical.records.domain.ports.out;

import com.jclinical.records.domain.model.PatientMedication;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PatientMedicationRepositoryPort {
    PatientMedication save(PatientMedication medication);
    List<PatientMedication> findByClinicIdAndPatientId(UUID clinicId, UUID patientId);
    Optional<PatientMedication> findByIdAndClinicId(UUID id, UUID clinicId);
    void deleteByIdAndClinicId(UUID id, UUID clinicId);
}
