package com.jclinical.accounting.infra.adapters.in.web.dto;

import com.jclinical.accounting.domain.model.IncomeStatementReport;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record IncomeStatementResponse(
        PeriodResponse period,
        PeriodResponse comparisonPeriod,
        SummaryResponse current,
        SummaryResponse previous,
        ChangeResponse change,
        List<AccountLineResponse> accounts,
        List<TrendPointResponse> trend) {

    public static IncomeStatementResponse from(IncomeStatementReport report) {
        return new IncomeStatementResponse(
                PeriodResponse.from(report.period()),
                PeriodResponse.from(report.comparisonPeriod()),
                SummaryResponse.from(report.current()),
                SummaryResponse.from(report.previous()),
                new ChangeResponse(
                        report.change().incomePercentage(),
                        report.change().expensesPercentage(),
                        report.change().netProfitPercentage()),
                report.accounts().stream().map(AccountLineResponse::from).toList(),
                report.trend().stream().map(TrendPointResponse::from).toList());
    }

    public record PeriodResponse(LocalDate from, LocalDate to) {
        private static PeriodResponse from(IncomeStatementReport.Period period) {
            return period == null ? null : new PeriodResponse(period.from(), period.to());
        }
    }

    public record SummaryResponse(
            BigDecimal income,
            BigDecimal directCosts,
            BigDecimal grossProfit,
            BigDecimal waste,
            BigDecimal otherExpenses,
            BigDecimal totalExpenses,
            BigDecimal netProfit,
            BigDecimal directCostPercentage,
            BigDecimal wastePercentage,
            BigDecimal otherExpensesPercentage,
            BigDecimal grossMarginPercentage,
            BigDecimal netMarginPercentage) {
        private static SummaryResponse from(IncomeStatementReport.Summary summary) {
            return new SummaryResponse(
                    summary.income(),
                    summary.directCosts(),
                    summary.grossProfit(),
                    summary.waste(),
                    summary.otherExpenses(),
                    summary.totalExpenses(),
                    summary.netProfit(),
                    summary.directCostPercentage(),
                    summary.wastePercentage(),
                    summary.otherExpensesPercentage(),
                    summary.grossMarginPercentage(),
                    summary.netMarginPercentage());
        }
    }

    public record ChangeResponse(
            BigDecimal incomePercentage,
            BigDecimal expensesPercentage,
            BigDecimal netProfitPercentage) {
    }

    public record AccountLineResponse(
            String accountCode,
            String accountName,
            String category,
            BigDecimal currentAmount,
            BigDecimal previousAmount,
            BigDecimal changePercentage,
            BigDecimal percentageOfIncome) {
        private static AccountLineResponse from(IncomeStatementReport.AccountLine line) {
            return new AccountLineResponse(
                    line.accountCode(),
                    line.accountName(),
                    line.category(),
                    line.currentAmount(),
                    line.previousAmount(),
                    line.changePercentage(),
                    line.percentageOfIncome());
        }
    }

    public record TrendPointResponse(
            LocalDate from,
            LocalDate to,
            BigDecimal income,
            BigDecimal expenses,
            BigDecimal netProfit) {
        private static TrendPointResponse from(IncomeStatementReport.TrendPoint point) {
            return new TrendPointResponse(
                    point.from(),
                    point.to(),
                    point.income(),
                    point.expenses(),
                    point.netProfit());
        }
    }
}
