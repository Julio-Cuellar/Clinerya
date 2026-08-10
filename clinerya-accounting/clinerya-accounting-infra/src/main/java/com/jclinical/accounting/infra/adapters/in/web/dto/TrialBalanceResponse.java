package com.jclinical.accounting.infra.adapters.in.web.dto;

import com.jclinical.accounting.domain.model.TrialBalanceReport;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TrialBalanceResponse(
        LocalDate from,
        LocalDate to,
        List<AccountResponse> accounts,
        TotalsResponse totals) {

    public static TrialBalanceResponse from(TrialBalanceReport report) {
        return new TrialBalanceResponse(report.from(), report.to(),
                report.accounts().stream().map(AccountResponse::from).toList(),
                new TotalsResponse(report.totals().initialDebit(), report.totals().initialCredit(),
                        report.totals().debit(), report.totals().credit(),
                        report.totals().finalDebit(), report.totals().finalCredit(),
                        report.totals().initialBalance(), report.totals().finalBalance()));
    }

    public record AccountResponse(
            String code, String name, String nature, String category,
            BigDecimal initialBalance, BigDecimal debit, BigDecimal credit, BigDecimal finalBalance,
            BigDecimal initialDebit, BigDecimal initialCredit, BigDecimal balanceAmount,
            String balanceNature, String closingSide, BigDecimal balancedTotal, BigDecimal activity,
            List<MovementResponse> movements) {
        private static AccountResponse from(TrialBalanceReport.AccountLine row) {
            return new AccountResponse(row.code(), row.name(), row.nature(), row.category(),
                    row.initialBalance(), row.debit(), row.credit(), row.finalBalance(),
                    row.initialDebit(), row.initialCredit(), row.balanceAmount(), row.balanceNature(),
                    row.closingSide(), row.balancedTotal(), row.activity(),
                    row.movements().stream().map(MovementResponse::from).toList());
        }
    }

    public record MovementResponse(
            UUID id, UUID journalEntryId, LocalDate date, String description,
            String sourceEventType, BigDecimal debit, BigDecimal credit) {
        private static MovementResponse from(TrialBalanceReport.Movement movement) {
            return new MovementResponse(movement.id(), movement.journalEntryId(), movement.date(),
                    movement.description(), movement.sourceEventType(), movement.debit(), movement.credit());
        }
    }

    public record TotalsResponse(
            BigDecimal initialDebit, BigDecimal initialCredit, BigDecimal debit, BigDecimal credit,
            BigDecimal finalDebit, BigDecimal finalCredit, BigDecimal initialBalance, BigDecimal finalBalance) {
    }
}
