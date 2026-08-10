package com.jclinical.accounting.infra.adapters.in.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OpeningBalanceSetupResponse(
        UUID id,
        UUID clinicId,
        LocalDate entryDate,
        BigDecimal cashOpeningAmount,
        BigDecimal inventoryOpeningAmount,
        BigDecimal totalOpeningAssets,
        String capitalAccountCode,
        String capitalAccountName,
        UUID journalEntryId,
        String notes,
        LocalDateTime createdAt,
        List<BankAccountResponse> bankAccounts
) {}
