package com.jclinical.cash.infra.adapters.in.web.dto;

import com.jclinical.cash.domain.model.CashExpenseStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CashExpenseResponse(
        UUID id,
        UUID clinicId,
        UUID cashSessionId,
        String concept,
        BigDecimal amount,
        UUID createdByStaffId,
        LocalDateTime createdAt,
        CashExpenseStatus status,
        UUID voidedByStaffId,
        LocalDateTime voidedAt,
        String voidReason
) {}
