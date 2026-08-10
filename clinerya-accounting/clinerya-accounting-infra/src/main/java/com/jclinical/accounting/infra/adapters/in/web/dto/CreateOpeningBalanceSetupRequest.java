package com.jclinical.accounting.infra.adapters.in.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreateOpeningBalanceSetupRequest(
        LocalDate entryDate,
        BigDecimal cashOpeningAmount,
        BigDecimal inventoryOpeningAmount,
        List<BankAccountOpeningRequest> bankAccounts,
        String notes
) {}
