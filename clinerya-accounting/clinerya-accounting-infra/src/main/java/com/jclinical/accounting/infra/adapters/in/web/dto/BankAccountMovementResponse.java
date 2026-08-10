package com.jclinical.accounting.infra.adapters.in.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record BankAccountMovementResponse(
        UUID journalEntryId,
        UUID journalLineId,
        LocalDate entryDate,
        String description,
        String sourceEventType,
        BigDecimal debit,
        BigDecimal credit,
        BigDecimal movementAmount,
        BigDecimal balanceAfter
) {}
