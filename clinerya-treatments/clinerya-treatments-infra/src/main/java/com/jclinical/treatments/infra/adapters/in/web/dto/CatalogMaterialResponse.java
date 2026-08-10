package com.jclinical.treatments.infra.adapters.in.web.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CatalogMaterialResponse(
    UUID id,
    UUID materialId,
    String materialName,
    BigDecimal typicalQuantity
) {}
