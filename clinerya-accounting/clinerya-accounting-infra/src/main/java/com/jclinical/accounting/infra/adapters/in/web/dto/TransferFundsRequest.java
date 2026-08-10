package com.jclinical.accounting.infra.adapters.in.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TransferFundsRequest(
        UUID sourceAccountId,
        UUID destinationAccountId,
        BigDecimal amount,
        LocalDate entryDate,
        String description
) {}
