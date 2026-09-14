package com.jclinical.records.domain.ports.out;

import com.jclinical.records.domain.model.ClinicalNoteDiagnosis;

import java.util.List;
import java.util.UUID;

public interface ClinicalNoteDiagnosisRepositoryPort {

    List<ClinicalNoteDiagnosis> saveAll(List<ClinicalNoteDiagnosis> diagnoses);

    List<ClinicalNoteDiagnosis> findByClinicalNoteIdAndClinicId(UUID clinicalNoteId, UUID clinicId);
}
