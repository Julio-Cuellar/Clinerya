package com.jclinical.treatments.infra.adapters.in.web.dto;

import java.math.BigDecimal;
import java.util.List;

public record CreateTreatmentCatalogItemRequest(
    String name,
    String category,
    String description,
    BigDecimal defaultPrice,
    Integer estimatedDurationMinutes,
    List<CatalogMaterialRequest> materials,
    /** FIXED o VARIES_BY_PATIENT; si no llega se toma FIXED (clientes anteriores). */
    String pricingType,
    Boolean availableInAssistant
) {}
