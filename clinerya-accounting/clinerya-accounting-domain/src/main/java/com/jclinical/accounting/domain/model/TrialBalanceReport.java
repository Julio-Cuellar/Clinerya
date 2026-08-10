package com.jclinical.accounting.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TrialBalanceReport(
        LocalDate from,
        LocalDate to,
        List<AccountLine> accounts,
        Totals totals) {

    public record AccountLine(
            String code,
            String name,
            String nature,
            String category,
            BigDecimal initialBalance,
            BigDecimal debit,
            BigDecimal credit,
            BigDecimal finalBalance,
            BigDecimal initialDebit,
            BigDecimal initialCredit,
            BigDecimal balanceAmount,
            String balanceNature,
            String closingSide,
            BigDecimal balancedTotal,
            BigDecimal activity,
            List<Movement> movements) {
    }

    public record Movement(
            UUID id,
            UUID journalEntryId,
            LocalDate date,
            String description,
            String sourceEventType,
            BigDecimal debit,
            BigDecimal credit) {
    }

    public record Totals(
            BigDecimal initialDebit,
            BigDecimal initialCredit,
            BigDecimal debit,
            BigDecimal credit,
            BigDecimal finalDebit,
            BigDecimal finalCredit,
            BigDecimal initialBalance,
            BigDecimal finalBalance) {
    }
}
