package com.jclinical.treatments.infra.adapters.in.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record TreatmentCatalogItemResponse(
    UUID id,
    UUID clinicId,
    String name,
    String category,
    String description,
    BigDecimal defaultPrice,
    Integer estimatedDurationMinutes,
    List<CatalogMaterialResponse> materials,
    boolean active,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
