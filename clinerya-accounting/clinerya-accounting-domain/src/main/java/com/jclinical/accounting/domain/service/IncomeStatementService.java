package com.jclinical.accounting.domain.service;

import com.jclinical.accounting.domain.model.IncomeStatementReport;
import com.jclinical.accounting.domain.model.JournalEntry;
import com.jclinical.accounting.domain.model.JournalLine;
import com.jclinical.accounting.domain.model.TrialBalanceReport;
import com.jclinical.accounting.domain.model.WasteReport;
import com.jclinical.accounting.domain.ports.in.GenerateAccountingReportsUseCase;
import com.jclinical.accounting.domain.ports.out.JournalEntryRepositoryPort;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class IncomeStatementService implements GenerateAccountingReportsUseCase {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final int MAX_TREND_BUCKETS = 6;
    private static final String DIRECT_COST_ACCOUNT = "51000";
    private static final String WASTE_ACCOUNT = "52100";

    private final JournalEntryRepositoryPort repository;

    public IncomeStatementService(JournalEntryRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    public IncomeStatementReport generateIncomeStatement(
            UUID clinicId,
            LocalDate from,
            LocalDate to,
            boolean includeComparison) {
        Objects.requireNonNull(clinicId, "La clinica es obligatoria.");
        Objects.requireNonNull(from, "La fecha inicial es obligatoria.");
        Objects.requireNonNull(to, "La fecha final es obligatoria.");
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la fecha final.");
        }

        long periodDays = ChronoUnit.DAYS.between(from, to) + 1;
        LocalDate comparisonTo = from.minusDays(1);
        LocalDate comparisonFrom = comparisonTo.minusDays(periodDays - 1);
        LocalDate queryFrom = includeComparison ? comparisonFrom : from;
        List<JournalEntry> entries = repository.findByClinicIdAndEntryDateBetween(clinicId, queryFrom, to);

        List<JournalEntry> currentEntries = inPeriod(entries, from, to);
        List<JournalEntry> previousEntries = includeComparison
                ? inPeriod(entries, comparisonFrom, comparisonTo)
                : List.of();
        Aggregation current = aggregate(currentEntries);
        Aggregation previous = aggregate(previousEntries);

        IncomeStatementReport.Summary currentSummary = current.toSummary();
        IncomeStatementReport.Summary previousSummary = previous.toSummary();
        return new IncomeStatementReport(
                new IncomeStatementReport.Period(from, to),
                includeComparison ? new IncomeStatementReport.Period(comparisonFrom, comparisonTo) : null,
                currentSummary,
                previousSummary,
                new IncomeStatementReport.Change(
                        percentageChange(currentSummary.income(), previousSummary.income()),
                        percentageChange(currentSummary.totalExpenses(), previousSummary.totalExpenses()),
                        percentageChange(currentSummary.netProfit(), previousSummary.netProfit())),
                accountLines(current, previous),
                trend(currentEntries, from, to));
    }

    @Override
    public TrialBalanceReport generateTrialBalance(UUID clinicId, LocalDate from, LocalDate to) {
        Objects.requireNonNull(clinicId, "La clinica es obligatoria.");
        Objects.requireNonNull(from, "La fecha inicial es obligatoria.");
        Objects.requireNonNull(to, "La fecha final es obligatoria.");
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la fecha final.");
        }

        Map<String, TrialAccount> accounts = new LinkedHashMap<>();
        for (JournalEntry entry : repository.findByClinicId(clinicId)) {
            if (entry.getLines() == null || entry.getEntryDate() == null) {
                continue;
            }
            boolean before = entry.getEntryDate().isBefore(from);
            boolean within = !entry.getEntryDate().isBefore(from) && !entry.getEntryDate().isAfter(to);
            if (!before && !within) {
                continue;
            }
            for (JournalLine line : entry.getLines()) {
                BigDecimal debit = value(line.getDebit());
                BigDecimal credit = value(line.getCredit());
                TrialAccount account = accounts.computeIfAbsent(line.getAccountCode(), code ->
                        new TrialAccount(code, line.getAccountName(), metadataOf(code)));
                if (before) {
                    account.initialSigned = account.initialSigned.add(account.isDebitNature()
                            ? debit.subtract(credit)
                            : credit.subtract(debit));
                } else if (within) {
                    account.debit = account.debit.add(debit);
                    account.credit = account.credit.add(credit);
                    if (debit.signum() != 0 || credit.signum() != 0) {
                        account.movements.add(new TrialBalanceReport.Movement(
                                line.getId(), entry.getId(), entry.getEntryDate(), entry.getDescription(),
                                entry.getSourceEventType(), debit, credit));
                    }
                }
            }
        }

        List<TrialBalanceReport.AccountLine> rows = accounts.values().stream()
                .filter(account -> account.initialSigned.signum() != 0
                        || account.debit.signum() != 0
                        || account.credit.signum() != 0)
                .sorted(Comparator.comparing(account -> account.code))
                .map(TrialAccount::toReport)
                .toList();

        BigDecimal initialDebit = rows.stream().map(TrialBalanceReport.AccountLine::initialDebit).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal initialCredit = rows.stream().map(TrialBalanceReport.AccountLine::initialCredit).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal debit = rows.stream().map(TrialBalanceReport.AccountLine::debit).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal credit = rows.stream().map(TrialBalanceReport.AccountLine::credit).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal finalDebit = rows.stream().map(row -> row.finalBalance().signum() >= 0 ? row.finalBalance() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal finalCredit = rows.stream().map(row -> row.finalBalance().signum() < 0 ? row.finalBalance().abs() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal initialBalance = rows.stream().map(TrialBalanceReport.AccountLine::initialBalance).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal finalBalance = rows.stream().map(TrialBalanceReport.AccountLine::finalBalance).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new TrialBalanceReport(from, to, rows,
                new TrialBalanceReport.Totals(initialDebit, initialCredit, debit, credit, finalDebit, finalCredit,
                        initialBalance, finalBalance));
    }

    @Override
    public WasteReport generateWasteReport(UUID clinicId, LocalDate from, LocalDate to) {
        Objects.requireNonNull(clinicId, "La clinica es obligatoria.");
        Objects.requireNonNull(from, "La fecha inicial es obligatoria.");
        Objects.requireNonNull(to, "La fecha final es obligatoria.");
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la fecha final.");
        }

        List<WasteReport.Line> lines = new ArrayList<>();
        for (JournalEntry entry : repository.findByClinicIdAndEntryDateBetween(clinicId, from, to)) {
            if (entry.getLines() == null) {
                continue;
            }
            BigDecimal amount = entry.getLines().stream()
                    .filter(line -> WASTE_ACCOUNT.equals(line.getAccountCode()))
                    .map(line -> value(line.getDebit()).subtract(value(line.getCredit())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (amount.signum() > 0) {
                lines.add(new WasteReport.Line(
                        entry.getId(), entry.getSourceEventId(), entry.getEntryDate(), entry.getDescription(), amount));
            }
        }

        BigDecimal totalAmount = lines.stream()
                .map(WasteReport.Line::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new WasteReport(from, to, totalAmount, lines.size(), lines);
    }

    private AccountMetadata metadataOf(String code) {
        if (code == null || code.isBlank()) return new AccountMetadata("DEBIT", "CUSTOM");
        return switch (code.substring(0, 1)) {
            case "1" -> new AccountMetadata("DEBIT", "ASSET");
            case "2" -> new AccountMetadata("CREDIT", "LIABILITY");
            case "3" -> new AccountMetadata("CREDIT", "EQUITY");
            case "4" -> new AccountMetadata("CREDIT", "INCOME");
            case "5" -> new AccountMetadata("DEBIT", "EXPENSE");
            default -> new AccountMetadata("DEBIT", "CUSTOM");
        };
    }

    private record AccountMetadata(String nature, String category) {}

    private static final class TrialAccount {
        private final String code;
        private final String name;
        private final AccountMetadata metadata;
        private BigDecimal initialSigned = BigDecimal.ZERO;
        private BigDecimal debit = BigDecimal.ZERO;
        private BigDecimal credit = BigDecimal.ZERO;
        private final List<TrialBalanceReport.Movement> movements = new ArrayList<>();

        private TrialAccount(String code, String name, AccountMetadata metadata) {
            this.code = code;
            this.name = name == null || name.isBlank() ? "Cuenta sin nombre" : name;
            this.metadata = metadata;
        }

        private boolean isDebitNature() { return "DEBIT".equals(metadata.nature()); }

        private TrialBalanceReport.AccountLine toReport() {
            BigDecimal initialDebit = initialSigned.signum() >= 0 && isDebitNature() ? initialSigned : BigDecimal.ZERO;
            BigDecimal initialCredit = initialSigned.signum() < 0 && isDebitNature() ? initialSigned.abs() : BigDecimal.ZERO;
            if (!isDebitNature()) {
                initialDebit = initialSigned.signum() < 0 ? initialSigned.abs() : BigDecimal.ZERO;
                initialCredit = initialSigned.signum() >= 0 ? initialSigned : BigDecimal.ZERO;
            }
            BigDecimal finalSigned = isDebitNature()
                    ? initialSigned.add(debit).subtract(credit)
                    : initialSigned.add(credit).subtract(debit);
            BigDecimal balanceAmount = finalSigned.abs();
            String balanceNature = isDebitNature()
                    ? (finalSigned.signum() >= 0 ? "DEBIT" : "CREDIT")
                    : (finalSigned.signum() >= 0 ? "CREDIT" : "DEBIT");
            return new TrialBalanceReport.AccountLine(
                    code, name, metadata.nature(), metadata.category(), initialSigned, debit, credit, finalSigned,
                    initialDebit, initialCredit, balanceAmount, balanceNature,
                    "DEBIT".equals(balanceNature) ? "CREDIT" : "DEBIT",
                    max(debit.add(initialDebit), credit.add(initialCredit)),
                    debit.add(credit), List.copyOf(movements));
        }

        private static BigDecimal max(BigDecimal left, BigDecimal right) { return left.max(right); }
    }

    private List<JournalEntry> inPeriod(List<JournalEntry> entries, LocalDate from, LocalDate to) {
        return entries.stream()
                .filter(entry -> entry.getEntryDate() != null)
                .filter(entry -> !entry.getEntryDate().isBefore(from) && !entry.getEntryDate().isAfter(to))
                .toList();
    }

    private Aggregation aggregate(List<JournalEntry> entries) {
        Aggregation aggregation = new Aggregation();
        for (JournalEntry entry : entries) {
            if (entry.getLines() == null) {
                continue;
            }
            for (JournalLine line : entry.getLines()) {
                Category category = categoryOf(line.getAccountCode());
                if (category == null) {
                    continue;
                }
                BigDecimal debit = value(line.getDebit());
                BigDecimal credit = value(line.getCredit());
                BigDecimal amount = category == Category.INCOME
                        ? credit.subtract(debit)
                        : debit.subtract(credit);
                aggregation.add(line.getAccountCode(), line.getAccountName(), category, amount);
            }
        }
        return aggregation;
    }

    private List<IncomeStatementReport.AccountLine> accountLines(Aggregation current, Aggregation previous) {
        Map<String, AccountTotal> combined = new LinkedHashMap<>();
        previous.accounts.forEach((code, total) -> combined.put(code, total));
        current.accounts.forEach((code, total) -> combined.put(code, total));
        BigDecimal income = current.total(Category.INCOME);

        return combined.entrySet().stream()
                .sorted(Comparator
                        .comparingInt((Map.Entry<String, AccountTotal> entry) -> entry.getValue().category.order)
                        .thenComparing(Map.Entry::getKey))
                .map(entry -> {
                    AccountTotal currentTotal = current.accounts.get(entry.getKey());
                    AccountTotal previousTotal = previous.accounts.get(entry.getKey());
                    AccountTotal metadata = currentTotal != null ? currentTotal : previousTotal;
                    BigDecimal currentAmount = currentTotal != null ? currentTotal.amount : BigDecimal.ZERO;
                    BigDecimal previousAmount = previousTotal != null ? previousTotal.amount : BigDecimal.ZERO;
                    return new IncomeStatementReport.AccountLine(
                            entry.getKey(),
                            metadata.name,
                            metadata.category.apiName,
                            currentAmount,
                            previousAmount,
                            percentageChange(currentAmount, previousAmount),
                            percentageOf(currentAmount, income));
                })
                .toList();
    }

    private List<IncomeStatementReport.TrendPoint> trend(
            List<JournalEntry> entries,
            LocalDate from,
            LocalDate to) {
        int totalDays = Math.toIntExact(ChronoUnit.DAYS.between(from, to) + 1);
        int bucketCount = Math.min(MAX_TREND_BUCKETS, totalDays);
        List<List<JournalEntry>> buckets = new ArrayList<>();
        for (int index = 0; index < bucketCount; index++) {
            buckets.add(new ArrayList<>());
        }
        for (JournalEntry entry : entries) {
            int elapsedDays = Math.toIntExact(ChronoUnit.DAYS.between(from, entry.getEntryDate()));
            int bucketIndex = Math.min(
                    bucketCount - 1,
                    (elapsedDays * bucketCount + totalDays - 1) / totalDays);
            buckets.get(bucketIndex).add(entry);
        }

        List<IncomeStatementReport.TrendPoint> result = new ArrayList<>();
        for (int index = 0; index < bucketCount; index++) {
            LocalDate bucketFrom = from.plusDays((long) index * totalDays / bucketCount);
            LocalDate bucketTo = index == bucketCount - 1
                    ? to
                    : from.plusDays((long) (index + 1) * totalDays / bucketCount).minusDays(1);
            IncomeStatementReport.Summary summary = aggregate(buckets.get(index)).toSummary();
            result.add(new IncomeStatementReport.TrendPoint(
                    bucketFrom,
                    bucketTo,
                    summary.income(),
                    summary.totalExpenses(),
                    summary.netProfit()));
        }
        return result;
    }

    private Category categoryOf(String accountCode) {
        if (accountCode == null) {
            return null;
        }
        if (accountCode.startsWith("4")) {
            return Category.INCOME;
        }
        if (DIRECT_COST_ACCOUNT.equals(accountCode)) {
            return Category.DIRECT_COST;
        }
        if (WASTE_ACCOUNT.equals(accountCode)) {
            return Category.WASTE;
        }
        return accountCode.startsWith("5") ? Category.OTHER_EXPENSE : null;
    }

    private static BigDecimal value(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private static BigDecimal percentageOf(BigDecimal amount, BigDecimal base) {
        if (base.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return amount.multiply(ONE_HUNDRED).divide(base, 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal percentageChange(BigDecimal current, BigDecimal previous) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : null;
        }
        return current.subtract(previous)
                .multiply(ONE_HUNDRED)
                .divide(previous.abs(), 2, RoundingMode.HALF_UP);
    }

    private enum Category {
        INCOME("INCOME", 0),
        DIRECT_COST("DIRECT_COST", 1),
        WASTE("WASTE", 2),
        OTHER_EXPENSE("OTHER_EXPENSE", 3);

        private final String apiName;
        private final int order;

        Category(String apiName, int order) {
            this.apiName = apiName;
            this.order = order;
        }
    }

    private static final class AccountTotal {
        private final String name;
        private final Category category;
        private BigDecimal amount;

        private AccountTotal(String name, Category category) {
            this.name = name == null || name.isBlank() ? "Cuenta sin nombre" : name;
            this.category = category;
            this.amount = BigDecimal.ZERO;
        }
    }

    private static final class Aggregation {
        private final Map<String, AccountTotal> accounts = new HashMap<>();

        private void add(String code, String name, Category category, BigDecimal amount) {
            AccountTotal total = accounts.computeIfAbsent(code, ignored -> new AccountTotal(name, category));
            total.amount = total.amount.add(amount);
        }

        private BigDecimal total(Category category) {
            return accounts.values().stream()
                    .filter(total -> total.category == category)
                    .map(total -> total.amount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        private IncomeStatementReport.Summary toSummary() {
            BigDecimal income = total(Category.INCOME);
            BigDecimal directCosts = total(Category.DIRECT_COST);
            BigDecimal grossProfit = income.subtract(directCosts);
            BigDecimal waste = total(Category.WASTE);
            BigDecimal otherExpenses = total(Category.OTHER_EXPENSE);
            BigDecimal totalExpenses = directCosts.add(waste).add(otherExpenses);
            BigDecimal netProfit = income.subtract(totalExpenses);
            return new IncomeStatementReport.Summary(
                    income,
                    directCosts,
                    grossProfit,
                    waste,
                    otherExpenses,
                    totalExpenses,
                    netProfit,
                    percentageOf(directCosts, income),
                    percentageOf(waste, income),
                    percentageOf(otherExpenses, income),
                    percentageOf(grossProfit, income),
                    percentageOf(netProfit, income));
        }
    }
}
