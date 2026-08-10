package com.jclinical.records.infra.adapters.in.web.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record MedicalHistoryResponse(
    UUID id,
    UUID patientId,
    UUID clinicId,
    UUID templateId,
    String answersJson,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
