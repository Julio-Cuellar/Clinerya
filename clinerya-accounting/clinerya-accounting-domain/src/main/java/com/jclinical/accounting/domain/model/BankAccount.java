package com.jclinical.accounting.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankAccount {
    private UUID id;
    private UUID clinicId;
    private UUID openingBalanceSetupId;
    private UUID openingJournalEntryId;
    private String accountCode;
    private BankAccountType accountType;
    private OperationalAccountKind accountKind;
    private String bankName;
    private String alias;
    private String accountLast4;
    private String currency;
    private BigDecimal openingBalance;
    private LocalDate openingDate;
    private LocalDate creditCutoffDate;
    private LocalDate creditPaymentDueDate;
    private BigDecimal creditLimit;
    private BigDecimal creditCurrentAmount;
    private BigDecimal creditMinimumPayment;
    private BigDecimal creditNoInterestPayment;
    private BigDecimal creditCurrentPaymentDue;
    private boolean active;
    private LocalDateTime deactivatedAt;
    private String deactivationReason;
    private LocalDateTime lastModifiedAt;
    private String lastModificationReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String displayName() {
        String base = alias != null && !alias.isBlank() ? alias.trim() : bankName;
        if (accountLast4 == null || accountLast4.isBlank()) {
            return base;
        }
        return base + " ****" + accountLast4;
    }
}
