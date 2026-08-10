package com.jclinical.records.infra.adapters.in.web.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record HistoryTemplateResponse(
    UUID id,
    UUID clinicId,
    String name,
    String description,
    String schemaJson,
    boolean active,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
