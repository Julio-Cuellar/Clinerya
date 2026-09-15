package com.jclinical.accounting.domain.service;

import com.jclinical.accounting.domain.model.IncomeStatementReport;
import com.jclinical.accounting.domain.model.JournalEntry;
import com.jclinical.accounting.domain.model.JournalLine;
import com.jclinical.accounting.domain.model.TrialBalanceReport;
import com.jclinical.accounting.domain.model.WasteReport;
import com.jclinical.accounting.domain.ports.in.GenerateAccountingReportsUseCase;
import com.jclinical.accounting.domain.ports.out.JournalEntryRepositoryPort;
import com.jclinical.core.security.ClinicAccessDeniedException;
import com.jclinical.core.security.StaffPermission;
import com.jclinical.core.security.StaffPermissionCheckerPort;

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
    private static final String DEBIT_NATURE = "DEBIT";
    private static final String CREDIT_NATURE = "CREDIT";
    private static final String CUSTOM_CATEGORY = "CUSTOM";
    private static final String REQUIRED_CLINIC_MESSAGE = "La clinica es obligatoria.";
    private static final String REQUIRED_FROM_MESSAGE = "La fecha inicial es obligatoria.";
    private static final String REQUIRED_TO_MESSAGE = "La fecha final es obligatoria.";
    private static final String INVALID_PERIOD_MESSAGE = "La fecha inicial no puede ser posterior a la fecha final.";

    private final JournalEntryRepositoryPort repository;
    private final StaffPermissionCheckerPort permissionChecker;

    public IncomeStatementService(JournalEntryRepositoryPort repository, StaffPermissionCheckerPort permissionChecker) {
        this.repository = repository;
        this.permissionChecker = permissionChecker;
    }

    private void requireViewReports(UUID clinicId, UUID actingUserId) {
        if (actingUserId == null) {
            throw new ClinicAccessDeniedException("Usuario no autenticado.");
        }
        if (!permissionChecker.hasPermission(clinicId, actingUserId, StaffPermission.VIEW_FINANCIAL_REPORTS)) {
            throw new ClinicAccessDeniedException("No tienes permiso para consultar los reportes financieros de esta clinica.");
        }
    }

    @Override
    public IncomeStatementReport generateIncomeStatement(
            UUID actingUserId,
            UUID clinicId,
            LocalDate from,
            LocalDate to,
            boolean includeComparison) {
        requireViewReports(clinicId, actingUserId);
        validatePeriod(clinicId, from, to);

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
    public TrialBalanceReport generateTrialBalance(UUID actingUserId, UUID clinicId, LocalDate from, LocalDate to) {
        requireViewReports(clinicId, actingUserId);
        validatePeriod(clinicId, from, to);

        Map<String, TrialAccount> accounts = new LinkedHashMap<>();
        for (JournalEntry entry : repository.findByClinicId(clinicId)) {
            if (isTrialEntry(entry, to)) {
                for (JournalLine line : entry.getLines()) {
                    applyTrialLine(accounts, entry, line, from);
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
    public WasteReport generateWasteReport(UUID actingUserId, UUID clinicId, LocalDate from, LocalDate to) {
        requireViewReports(clinicId, actingUserId);
        validatePeriod(clinicId, from, to);

        List<WasteReport.Line> lines = new ArrayList<>();
        repository.findByClinicIdAndEntryDateBetween(clinicId, from, to).stream()
                .filter(entry -> entry.getLines() != null)
                .forEach(entry -> {
                    BigDecimal amount = wasteAmount(entry);
            if (amount.signum() > 0) {
                lines.add(new WasteReport.Line(
                        entry.getId(), entry.getSourceEventId(), entry.getEntryDate(), entry.getDescription(), amount));
            }
                });

        BigDecimal totalAmount = lines.stream()
                .map(WasteReport.Line::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new WasteReport(from, to, totalAmount, lines.size(), lines);
    }

    private AccountMetadata metadataOf(String code) {
        if (code == null || code.isBlank()) {
            return new AccountMetadata(DEBIT_NATURE, CUSTOM_CATEGORY);
        }
        return switch (code.substring(0, 1)) {
            case "1" -> new AccountMetadata(DEBIT_NATURE, "ASSET");
            case "2" -> new AccountMetadata(CREDIT_NATURE, "LIABILITY");
            case "3" -> new AccountMetadata(CREDIT_NATURE, "EQUITY");
            case "4" -> new AccountMetadata(CREDIT_NATURE, "INCOME");
            case "5" -> new AccountMetadata(DEBIT_NATURE, "EXPENSE");
            default -> new AccountMetadata(DEBIT_NATURE, CUSTOM_CATEGORY);
        };
    }

    private void validatePeriod(UUID clinicId, LocalDate from, LocalDate to) {
        Objects.requireNonNull(clinicId, REQUIRED_CLINIC_MESSAGE);
        Objects.requireNonNull(from, REQUIRED_FROM_MESSAGE);
        Objects.requireNonNull(to, REQUIRED_TO_MESSAGE);
        if (from.isAfter(to)) {
            throw new IllegalArgumentException(INVALID_PERIOD_MESSAGE);
        }
    }

    private boolean isTrialEntry(JournalEntry entry, LocalDate to) {
        return entry.getLines() != null
                && entry.getEntryDate() != null
                && !entry.getEntryDate().isAfter(to);
    }

    private BigDecimal wasteAmount(JournalEntry entry) {
        return entry.getLines().stream()
                .filter(line -> WASTE_ACCOUNT.equals(line.getAccountCode()))
                .map(line -> value(line.getDebit()).subtract(value(line.getCredit())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void applyTrialLine(
            Map<String, TrialAccount> accounts,
            JournalEntry entry,
            JournalLine line,
            LocalDate from) {
        BigDecimal debit = value(line.getDebit());
        BigDecimal credit = value(line.getCredit());
        TrialAccount account = accounts.computeIfAbsent(line.getAccountCode(), code ->
                new TrialAccount(code, line.getAccountName(), metadataOf(code)));
        if (entry.getEntryDate().isBefore(from)) {
            account.addInitialBalance(debit, credit);
            return;
        }
        account.addMovement(entry, line, debit, credit);
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

        private boolean isDebitNature() { return DEBIT_NATURE.equals(metadata.nature()); }

        private void addInitialBalance(BigDecimal debit, BigDecimal credit) {
            BigDecimal amount = isDebitNature() ? debit.subtract(credit) : credit.subtract(debit);
            initialSigned = initialSigned.add(amount);
        }

        private void addMovement(JournalEntry entry, JournalLine line, BigDecimal movementDebit, BigDecimal movementCredit) {
            debit = debit.add(movementDebit);
            credit = credit.add(movementCredit);
            if (movementDebit.signum() != 0 || movementCredit.signum() != 0) {
                movements.add(new TrialBalanceReport.Movement(
                        line.getId(), entry.getId(), entry.getEntryDate(), entry.getDescription(),
                        entry.getSourceEventType(), movementDebit, movementCredit));
            }
        }

        private TrialBalanceReport.AccountLine toReport() {
            BigDecimal initialDebit = initialDebit();
            BigDecimal initialCredit = initialCredit();
            BigDecimal finalSigned = isDebitNature()
                    ? initialSigned.add(debit).subtract(credit)
                    : initialSigned.add(credit).subtract(debit);
            BigDecimal balanceAmount = finalSigned.abs();
            String balanceNature = balanceNature(finalSigned);
            return new TrialBalanceReport.AccountLine(
                    code, name, metadata.nature(), metadata.category(), initialSigned, debit, credit, finalSigned,
                    initialDebit, initialCredit, balanceAmount, balanceNature,
                    oppositeNature(balanceNature),
                    max(debit.add(initialDebit), credit.add(initialCredit)),
                    debit.add(credit), List.copyOf(movements));
        }

        private static BigDecimal max(BigDecimal left, BigDecimal right) { return left.max(right); }

        private BigDecimal initialDebit() {
            if (isDebitNature()) {
                return initialSigned.signum() >= 0 ? initialSigned : BigDecimal.ZERO;
            }
            return initialSigned.signum() < 0 ? initialSigned.abs() : BigDecimal.ZERO;
        }

        private BigDecimal initialCredit() {
            if (isDebitNature()) {
                return initialSigned.signum() < 0 ? initialSigned.abs() : BigDecimal.ZERO;
            }
            return initialSigned.signum() >= 0 ? initialSigned : BigDecimal.ZERO;
        }

        private String balanceNature(BigDecimal finalSigned) {
            boolean positive = finalSigned.signum() >= 0;
            if (isDebitNature()) {
                return positive ? DEBIT_NATURE : CREDIT_NATURE;
            }
            return positive ? CREDIT_NATURE : DEBIT_NATURE;
        }

        private String oppositeNature(String nature) {
            return DEBIT_NATURE.equals(nature) ? CREDIT_NATURE : DEBIT_NATURE;
        }
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
            if (entry.getLines() != null) {
                for (JournalLine line : entry.getLines()) {
                    Category category = categoryOf(line.getAccountCode());
                    if (category != null) {
                        BigDecimal debit = value(line.getDebit());
                        BigDecimal credit = value(line.getCredit());
                        BigDecimal amount = category == Category.INCOME
                                ? credit.subtract(debit)
                                : debit.subtract(credit);
                        aggregation.add(line.getAccountCode(), line.getAccountName(), category, amount);
                    }
                }
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
            int bucketIndex = Math.clamp(
                    (elapsedDays * bucketCount + totalDays - 1) / totalDays,
                    0,
                    bucketCount - 1);
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
