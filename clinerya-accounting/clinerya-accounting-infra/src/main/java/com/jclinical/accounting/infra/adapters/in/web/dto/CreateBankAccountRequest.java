package com.jclinical.accounting.infra.adapters.in.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateBankAccountRequest(
        String bankName,
        String alias,
        String accountLast4,
        String currency,
        BigDecimal openingBalance,
        String accountType,
        String accountKind,
        LocalDate openingDate,
        LocalDate creditCutoffDate,
        LocalDate creditPaymentDueDate,
        BigDecimal creditLimit,
        BigDecimal creditCurrentAmount,
        BigDecimal creditMinimumPayment,
        BigDecimal creditNoInterestPayment,
        BigDecimal creditCurrentPaymentDue,
        String notes
) {}
