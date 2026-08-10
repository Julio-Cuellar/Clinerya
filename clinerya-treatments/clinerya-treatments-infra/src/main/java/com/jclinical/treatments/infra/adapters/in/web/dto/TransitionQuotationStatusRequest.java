package com.jclinical.treatments.infra.adapters.in.web.dto;

import com.jclinical.treatments.domain.model.QuotationStatus;

import java.util.UUID;

public record TransitionQuotationStatusRequest(
    UUID clinicId,
    QuotationStatus targetStatus
) {}
