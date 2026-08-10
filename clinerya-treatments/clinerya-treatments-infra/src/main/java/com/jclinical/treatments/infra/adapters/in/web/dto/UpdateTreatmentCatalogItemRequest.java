package com.jclinical.treatments.infra.adapters.in.web.dto;

import java.math.BigDecimal;
import java.util.List;

public record UpdateTreatmentCatalogItemRequest(
    String name,
    String category,
    String description,
    BigDecimal defaultPrice,
    Integer estimatedDurationMinutes,
    List<CatalogMaterialRequest> materials,
    boolean active
) {}
