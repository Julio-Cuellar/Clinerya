package com.jclinical.accounting.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpeningBalanceSetup {
    private UUID id;
    private UUID clinicId;
    private LocalDate entryDate;
    private BigDecimal cashOpeningAmount;
    private BigDecimal inventoryOpeningAmount;
    private BigDecimal totalOpeningAssets;
    private String capitalAccountCode;
    private String capitalAccountName;
    private UUID journalEntryId;
    private String notes;
    private LocalDateTime createdAt;

    @Builder.Default
    private List<BankAccount> bankAccounts = new ArrayList<>();
}
