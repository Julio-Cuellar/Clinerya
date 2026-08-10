package com.jclinical.accounting.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record IncomeStatementReport(
        Period period,
        Period comparisonPeriod,
        Summary current,
        Summary previous,
        Change change,
        List<AccountLine> accounts,
        List<TrendPoint> trend) {

    public record Period(LocalDate from, LocalDate to) {
    }

    public record Summary(
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
    }

    public record Change(
            BigDecimal incomePercentage,
            BigDecimal expensesPercentage,
            BigDecimal netProfitPercentage) {
    }

    public record AccountLine(
            String accountCode,
            String accountName,
            String category,
            BigDecimal currentAmount,
            BigDecimal previousAmount,
            BigDecimal changePercentage,
            BigDecimal percentageOfIncome) {
    }

    public record TrendPoint(
            LocalDate from,
            LocalDate to,
            BigDecimal income,
            BigDecimal expenses,
            BigDecimal netProfit) {
    }
}
