package com.jclinical.records.infra.adapters.in.web.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record MedicalHistoryVersionResponse(
    UUID id,
    UUID medicalHistoryId,
    int version,
    String answersJson,
    UUID changedByUserId,
    String changedByUserName,
    String ipAddress,
    String userAgent,
    LocalDateTime createdAt
) {}
