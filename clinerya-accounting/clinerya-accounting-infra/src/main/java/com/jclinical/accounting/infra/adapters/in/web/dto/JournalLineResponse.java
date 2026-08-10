package com.jclinical.accounting.infra.adapters.in.web.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record JournalLineResponse(
        UUID id,
        UUID bankAccountId,
        String accountCode,
        String accountName,
        BigDecimal debit,
        BigDecimal credit
) {}
