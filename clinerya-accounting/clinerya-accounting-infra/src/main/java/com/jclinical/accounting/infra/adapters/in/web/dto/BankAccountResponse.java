package com.jclinical.accounting.infra.adapters.in.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record BankAccountResponse(
        UUID id,
        UUID clinicId,
        UUID openingBalanceSetupId,
        UUID openingJournalEntryId,
        String accountCode,
        String accountType,
        String accountKind,
        String bankName,
        String alias,
        String accountLast4,
        String currency,
        BigDecimal openingBalance,
        LocalDate openingDate,
        LocalDate creditCutoffDate,
        LocalDate creditPaymentDueDate,
        BigDecimal creditLimit,
        BigDecimal creditCurrentAmount,
        BigDecimal creditMinimumPayment,
        BigDecimal creditNoInterestPayment,
        BigDecimal creditCurrentPaymentDue,
        boolean active,
        LocalDateTime deactivatedAt,
        String deactivationReason,
        LocalDateTime lastModifiedAt,
        String lastModificationReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
