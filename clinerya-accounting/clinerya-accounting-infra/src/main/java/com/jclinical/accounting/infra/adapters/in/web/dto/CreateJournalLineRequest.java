package com.jclinical.accounting.infra.adapters.in.web.dto;

import java.math.BigDecimal;

public record CreateJournalLineRequest(
    String accountCode,
    String accountName,
    BigDecimal debit,
    BigDecimal credit
) {}
