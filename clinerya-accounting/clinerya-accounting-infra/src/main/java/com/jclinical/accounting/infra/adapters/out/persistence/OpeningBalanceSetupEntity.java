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
@Table(name = "opening_balance_setups", schema = "accounting")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpeningBalanceSetupEntity {

    @Id
    private UUID id;

    @Column(name = "clinic_id", nullable = false, unique = true)
    private UUID clinicId;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "cash_opening_amount", nullable = false, precision = 38, scale = 2)
    private BigDecimal cashOpeningAmount;

    @Column(name = "inventory_opening_amount", nullable = false, precision = 38, scale = 2)
    private BigDecimal inventoryOpeningAmount;

    @Column(name = "total_opening_assets", nullable = false, precision = 38, scale = 2)
    private BigDecimal totalOpeningAssets;

    @Column(name = "capital_account_code", nullable = false, length = 50)
    private String capitalAccountCode;

    @Column(name = "capital_account_name", nullable = false)
    private String capitalAccountName;

    @Column(name = "journal_entry_id", nullable = false, unique = true)
    private UUID journalEntryId;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
