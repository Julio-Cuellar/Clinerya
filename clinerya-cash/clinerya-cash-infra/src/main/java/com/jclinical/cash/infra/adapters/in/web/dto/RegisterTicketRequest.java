package com.jclinical.cash.infra.adapters.in.web.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record RegisterTicketRequest(
        UUID patientId,
        UUID quotationId,
        String concept,
        UUID createdByStaffId,
        List<PaymentLineRequest> paymentLines,
        BigDecimal discountAmount,
        UUID discountAuthorizedByStaffId,
        String discountReason
) {}
