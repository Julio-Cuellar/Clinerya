package com.jclinical.treatments.infra.adapters.in.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record VisitResponse(
        UUID id,
        UUID clinicId,
        UUID patientId,
        UUID quotationId,
        LocalDate visitDate,
        UUID doctorId,
        String notes,
        List<VisitLineItemResponse> items,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public record VisitLineItemResponse(
            UUID id,
            UUID quotationItemId,
            List<VisitMaterialUsageResponse> materialsUsed
    ) {}

    public record VisitMaterialUsageResponse(
            UUID id,
            UUID materialId,
            String materialName,
            BigDecimal actualQuantity
    ) {}
}
