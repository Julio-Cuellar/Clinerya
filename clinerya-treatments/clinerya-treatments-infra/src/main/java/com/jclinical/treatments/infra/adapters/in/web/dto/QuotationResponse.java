package com.jclinical.treatments.infra.adapters.in.web.dto;

import com.jclinical.treatments.domain.model.QuotationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record QuotationResponse(
    UUID id,
    UUID clinicId,
    UUID patientId,
    UUID createdByUserId,
    LocalDate quotationDate,
    QuotationStatus status,
    String notes,
    LocalDate validUntil,
    List<QuotationItemResponse> items,
    BigDecimal grandTotal,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
