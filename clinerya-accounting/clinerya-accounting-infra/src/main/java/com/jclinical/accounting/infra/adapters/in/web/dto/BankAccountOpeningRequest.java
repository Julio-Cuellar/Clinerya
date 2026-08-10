package com.jclinical.accounting.infra.adapters.in.web.dto;

import java.math.BigDecimal;

public record BankAccountOpeningRequest(
        String bankName,
        String alias,
        String accountLast4,
        String currency,
        BigDecimal openingBalance,
        String accountType,
        String accountKind
) {}
