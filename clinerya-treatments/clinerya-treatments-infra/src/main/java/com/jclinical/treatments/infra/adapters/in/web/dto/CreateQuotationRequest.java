package com.jclinical.treatments.infra.adapters.in.web.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateQuotationRequest(
    UUID clinicId,
    UUID createdByUserId,
    LocalDate quotationDate,
    String notes,
    LocalDate validUntil,
    List<QuotationItemRequest> items
) {}
