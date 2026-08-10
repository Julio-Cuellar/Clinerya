package com.jclinical.treatments.infra.adapters.in.web.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record MaterialLineResponse(
    UUID id,
    UUID materialId,
    String materialName,
    BigDecimal estimatedQuantity,
    BigDecimal unitCostAtQuote,
    BigDecimal estimatedCost
) {}
