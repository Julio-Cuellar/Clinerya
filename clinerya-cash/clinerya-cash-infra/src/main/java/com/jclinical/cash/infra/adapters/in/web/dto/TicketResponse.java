package com.jclinical.cash.infra.adapters.in.web.dto;

import com.jclinical.cash.domain.model.TicketStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record TicketResponse(
        UUID id,
        UUID clinicId,
        UUID cashSessionId,
        UUID patientId,
        UUID quotationId,
        Integer folio,
        BigDecimal totalAmount,
        String concept,
        UUID createdByStaffId,
        LocalDateTime createdAt,
        TicketStatus status,
        UUID voidedByStaffId,
        LocalDateTime voidedAt,
        String voidReason,
        BigDecimal discountAmount,
        UUID discountAuthorizedByStaffId,
        String discountReason,
        List<PaymentLineResponse> paymentLines
) {}
