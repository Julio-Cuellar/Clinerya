package com.jclinical.accounting.infra.adapters.in.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateBankAccountRequest(
        String bankName,
        String alias,
        String accountLast4,
        String currency,
        String accountType,
        String accountKind,
        LocalDate creditCutoffDate,
        LocalDate creditPaymentDueDate,
        BigDecimal creditLimit,
        BigDecimal creditCurrentAmount,
        BigDecimal creditMinimumPayment,
        BigDecimal creditNoInterestPayment,
        BigDecimal creditCurrentPaymentDue,
        String reason
) {}
