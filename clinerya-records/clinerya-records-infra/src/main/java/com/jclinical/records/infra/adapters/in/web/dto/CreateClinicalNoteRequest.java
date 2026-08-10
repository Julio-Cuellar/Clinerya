package com.jclinical.records.infra.adapters.in.web.dto;

import com.jclinical.records.domain.model.NoteStatus;

import java.util.UUID;

public record CreateClinicalNoteRequest(
    UUID clinicId,
    UUID doctorId,
    String subjective,
    String objective,
    Double temperature,
    String bloodPressure,
    Integer heartRate,
    Integer respiratoryRate,
    Double weight,
    Double height,
    Integer oxygenSaturation,
    String assessment,
    String plan,
    NoteStatus status
) {}
