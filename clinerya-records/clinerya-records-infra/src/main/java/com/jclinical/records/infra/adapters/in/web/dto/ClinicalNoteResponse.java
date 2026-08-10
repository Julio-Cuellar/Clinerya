package com.jclinical.records.infra.adapters.in.web.dto;

import com.jclinical.records.domain.model.NoteStatus;
import com.jclinical.records.domain.model.VitalSigns;

import java.time.LocalDateTime;
import java.util.UUID;

public record ClinicalNoteResponse(
    UUID id,
    UUID patientId,
    UUID clinicId,
    UUID doctorId,
    String subjective,
    String objective,
    VitalSigns vitalSigns,
    String assessment,
    String plan,
    NoteStatus status,
    UUID authoredByExternalUserId,
    LocalDateTime signedAt,
    UUID signedByUserId,
    String documentHash,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
