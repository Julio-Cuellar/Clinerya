package com.jclinical.treatments.infra.adapters.in.web.dto;

import com.jclinical.treatments.domain.model.ItemProgressStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record QuotationItemResponse(
    UUID id,
    UUID catalogItemId,
    String description,
    Integer toothNumber,
    BigDecimal laborCharge,
    List<MaterialLineResponse> materials,
    BigDecimal materialsTotal,
    BigDecimal discountPercentage,
    ItemProgressStatus progressStatus,
    BigDecimal subtotal
) {}
