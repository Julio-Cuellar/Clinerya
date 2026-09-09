package com.jclinical.records.infra.adapters.in.web.dto;

import com.jclinical.records.domain.model.ClinicalNoteAddendum;

import java.time.LocalDateTime;
import java.util.UUID;

public record ClinicalNoteAddendumResponse(
    UUID id,
    UUID clinicalNoteId,
    UUID clinicId,
    UUID patientId,
    UUID createdByUserId,
    String createdByUserName,
    String content,
    LocalDateTime createdAt
) {
    public static ClinicalNoteAddendumResponse from(ClinicalNoteAddendum addendum) {
        return new ClinicalNoteAddendumResponse(
                addendum.getId(),
                addendum.getClinicalNoteId(),
                addendum.getClinicId(),
                addendum.getPatientId(),
                addendum.getCreatedByUserId(),
                addendum.getCreatedByUserName(),
                addendum.getContent(),
                addendum.getCreatedAt()
        );
    }
}
