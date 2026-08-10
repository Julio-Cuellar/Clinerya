package com.jclinical.accounting.infra.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bank_accounts", schema = "accounting")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankAccountEntity {

    @Id
    private UUID id;

    @Column(name = "clinic_id", nullable = false)
    private UUID clinicId;

    @Column(name = "opening_balance_setup_id")
    private UUID openingBalanceSetupId;

    @Column(name = "opening_journal_entry_id")
    private UUID openingJournalEntryId;

    @Column(name = "account_code", nullable = false, length = 50)
    private String accountCode;

    @Column(name = "account_type", nullable = false, length = 20)
    private String accountType;

    @Column(name = "account_kind", nullable = false, length = 30)
    private String accountKind;

    @Column(name = "bank_name", nullable = false, length = 120)
    private String bankName;

    @Column(nullable = false, length = 120)
    private String alias;

    @Column(name = "account_last4", length = 4)
    private String accountLast4;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "opening_balance", nullable = false, precision = 38, scale = 2)
    private BigDecimal openingBalance;

    @Column(name = "opening_date", nullable = false)
    private LocalDate openingDate;

    @Column(name = "credit_cutoff_date")
    private LocalDate creditCutoffDate;

    @Column(name = "credit_payment_due_date")
    private LocalDate creditPaymentDueDate;

    @Column(name = "credit_limit", precision = 38, scale = 2)
    private BigDecimal creditLimit;

    @Column(name = "credit_current_amount", precision = 38, scale = 2)
    private BigDecimal creditCurrentAmount;

    @Column(name = "credit_minimum_payment", precision = 38, scale = 2)
    private BigDecimal creditMinimumPayment;

    @Column(name = "credit_no_interest_payment", precision = 38, scale = 2)
    private BigDecimal creditNoInterestPayment;

    @Column(name = "credit_current_payment_due", precision = 38, scale = 2)
    private BigDecimal creditCurrentPaymentDue;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "deactivated_at")
    private LocalDateTime deactivatedAt;

    @Column(name = "deactivation_reason", columnDefinition = "TEXT")
    private String deactivationReason;

    @Column(name = "last_modified_at")
    private LocalDateTime lastModifiedAt;

    @Column(name = "last_modification_reason", columnDefinition = "TEXT")
    private String lastModificationReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
