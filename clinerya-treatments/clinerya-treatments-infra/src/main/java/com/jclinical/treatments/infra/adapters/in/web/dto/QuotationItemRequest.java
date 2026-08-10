package com.jclinical.treatments.infra.adapters.in.web.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record QuotationItemRequest(
    UUID catalogItemId,
    String description,
    Integer toothNumber,
    BigDecimal laborCharge,
    List<MaterialLineRequest> materials,
    BigDecimal discountPercentage
) {}
