package com.jclinical.cash.infra.adapters.in.web.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record RegisterCashExpenseRequest(
        String concept,
        BigDecimal amount,
        UUID createdByStaffId
) {}
