package com.jclinical.treatments.infra.adapters.in.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateVisitRequest(
        UUID clinicId,
        LocalDate visitDate,
        UUID doctorId,
        String notes,
        List<CreateVisitLineItemRequest> items
) {
    public record CreateVisitLineItemRequest(
            UUID quotationItemId,
            List<CreateVisitMaterialUsageRequest> materialsUsed
    ) {}

    public record CreateVisitMaterialUsageRequest(
            UUID materialId,
            String materialName,
            BigDecimal actualQuantity
    ) {}
}
