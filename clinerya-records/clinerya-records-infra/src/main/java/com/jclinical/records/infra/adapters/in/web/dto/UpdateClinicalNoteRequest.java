package com.jclinical.records.infra.adapters.in.web.dto;

import java.util.UUID;

public record UpdateClinicalNoteRequest(
    UUID clinicId,
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
    String plan
) {}
