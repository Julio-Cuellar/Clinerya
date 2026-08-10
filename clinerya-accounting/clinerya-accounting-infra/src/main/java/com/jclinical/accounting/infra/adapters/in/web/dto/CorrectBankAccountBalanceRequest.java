package com.jclinical.accounting.infra.adapters.in.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CorrectBankAccountBalanceRequest(
        LocalDate entryDate,
        BigDecimal correctedBalance,
        String reason
) {}
