package com.jclinical.treatments.infra.adapters.in.web.dto;

import java.time.LocalDate;
import java.util.UUID;

public record UpdateQuotationHeaderRequest(
    UUID clinicId,
    String notes,
    LocalDate validUntil,
    LocalDate quotationDate
) {}
