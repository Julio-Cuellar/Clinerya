package com.jclinical.treatments.infra.adapters.in.web.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CatalogMaterialRequest(
    UUID materialId,
    BigDecimal typicalQuantity
) {}
