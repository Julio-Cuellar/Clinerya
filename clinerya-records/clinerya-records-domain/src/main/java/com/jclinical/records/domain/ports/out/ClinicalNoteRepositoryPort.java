package com.jclinical.records.domain.ports.out;

import com.jclinical.records.domain.model.ClinicalNote;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClinicalNoteRepositoryPort {
    Optional<ClinicalNote> findByIdAndPatientIdAndClinicId(UUID id, UUID patientId, UUID clinicId);
    List<ClinicalNote> findByPatientIdAndClinicIdOrderByCreatedAtDesc(UUID patientId, UUID clinicId);
    ClinicalNote save(ClinicalNote clinicalNote);
}
