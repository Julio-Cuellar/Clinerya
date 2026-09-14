package com.jclinical.records.infra.adapters.in.web.dto;

import com.jclinical.records.domain.model.ClinicalNoteDiagnosis;
import com.jclinical.records.domain.model.DiagnosisKind;

import java.time.LocalDateTime;
import java.util.UUID;

public record ClinicalNoteDiagnosisResponse(
    UUID id,
    UUID clinicalNoteId,
    UUID clinicId,
    String icd10Code,
    DiagnosisKind kind,
    LocalDateTime createdAt
) {
    public static ClinicalNoteDiagnosisResponse from(ClinicalNoteDiagnosis diagnosis) {
        return new ClinicalNoteDiagnosisResponse(
                diagnosis.getId(),
                diagnosis.getClinicalNoteId(),
                diagnosis.getClinicId(),
                diagnosis.getIcd10Code(),
                diagnosis.getKind(),
                diagnosis.getCreatedAt()
        );
    }
}
