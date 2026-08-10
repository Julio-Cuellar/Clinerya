package com.jclinical.cash.infra.adapters.in.web.dto;

import com.jclinical.cash.domain.model.CashSessionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CashSessionResponse(
        UUID id,
        UUID clinicId,
        UUID openedByStaffId,
        LocalDateTime openedAt,
        BigDecimal openingAmount,
        UUID closedByStaffId,
        LocalDateTime closedAt,
        BigDecimal countedCashAmount,
        BigDecimal expectedCashAmount,
        BigDecimal cashDifference,
        CashSessionStatus status
) {}
