package com.jclinical.accounting.domain.service;

import com.jclinical.accounting.domain.model.IncomeStatementReport;
import com.jclinical.accounting.domain.model.JournalEntry;
import com.jclinical.accounting.domain.model.JournalLine;
import com.jclinical.accounting.domain.model.JournalQueryResult;
import com.jclinical.accounting.domain.ports.out.JournalEntryRepositoryPort;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class IncomeStatementServiceTest {

    private final InMemoryJournalRepository repository = new InMemoryJournalRepository();
    private final IncomeStatementService service = new IncomeStatementService(repository);

    @Test
    void calculatesCurrentPeriodComparisonAccountsAndTrend() {
        UUID clinicId = UUID.randomUUID();
        repository.entries.add(entry(clinicId, "2026-07-02",
                line("41000", "Ingresos por servicios", "0", "1000"),
                line("11100", "Caja", "1000", "0")));
        repository.entries.add(entry(clinicId, "2026-07-04",
                line("51000", "Costo de insumos", "200", "0"),
                line("12100", "Inventario", "0", "200")));
        repository.entries.add(entry(clinicId, "2026-07-05",
                line("52100", "Mermas", "25", "0"),
                line("12100", "Inventario", "0", "25")));
        repository.entries.add(entry(clinicId, "2026-07-06",
                line("52200", "Gastos generales", "75", "0"),
                line("11100", "Caja", "0", "75")));
        repository.entries.add(entry(clinicId, "2026-06-24",
                line("41000", "Ingresos por servicios", "0", "800"),
                line("11100", "Caja", "800", "0")));
        repository.entries.add(entry(clinicId, "2026-06-26",
                line("51000", "Costo de insumos", "160", "0"),
                line("12100", "Inventario", "0", "160")));

        IncomeStatementReport report = service.generateIncomeStatement(
                clinicId,
                LocalDate.parse("2026-07-01"),
                LocalDate.parse("2026-07-07"),
                true);

        assertEquals(new BigDecimal("1000"), report.current().income());
        assertEquals(new BigDecimal("200"), report.current().directCosts());
        assertEquals(new BigDecimal("800"), report.current().grossProfit());
        assertEquals(new BigDecimal("25"), report.current().waste());
        assertEquals(new BigDecimal("75"), report.current().otherExpenses());
        assertEquals(new BigDecimal("300"), report.current().totalExpenses());
        assertEquals(new BigDecimal("700"), report.current().netProfit());
        assertEquals(new BigDecimal("70.00"), report.current().netMarginPercentage());

        assertEquals(LocalDate.parse("2026-06-24"), report.comparisonPeriod().from());
        assertEquals(LocalDate.parse("2026-06-30"), report.comparisonPeriod().to());
        assertEquals(new BigDecimal("800"), report.previous().income());
        assertEquals(new BigDecimal("640"), report.previous().netProfit());
        assertEquals(new BigDecimal("25.00"), report.change().incomePercentage());
        assertEquals(new BigDecimal("9.38"), report.change().netProfitPercentage());
        assertEquals(4, report.accounts().size());
        assertEquals(6, report.trend().size());
        assertEquals(new BigDecimal("1000"), report.trend().stream()
                .map(IncomeStatementReport.TrendPoint::income)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        assertEquals(new BigDecimal("300"), report.trend().stream()
                .map(IncomeStatementReport.TrendPoint::expenses)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    @Test
    void omitsComparisonAndMarksNewActivityWithoutAnArtificialPercentage() {
        UUID clinicId = UUID.randomUUID();
        repository.entries.add(entry(clinicId, "2026-07-01",
                line("41000", "Ingresos", "0", "100")));

        IncomeStatementReport report = service.generateIncomeStatement(
                clinicId,
                LocalDate.parse("2026-07-01"),
                LocalDate.parse("2026-07-01"),
                false);

        assertNull(report.comparisonPeriod());
        assertNull(report.change().incomePercentage());
        assertEquals(BigDecimal.ZERO, report.previous().income());
    }

    private static JournalEntry entry(UUID clinicId, String date, JournalLine... lines) {
        return JournalEntry.builder()
                .id(UUID.randomUUID())
                .clinicId(clinicId)
                .entryDate(LocalDate.parse(date))
                .lines(List.of(lines))
                .build();
    }

    private static JournalLine line(String code, String name, String debit, String credit) {
        return JournalLine.builder()
                .accountCode(code)
                .accountName(name)
                .debit(new BigDecimal(debit))
                .credit(new BigDecimal(credit))
                .build();
    }

    private static final class InMemoryJournalRepository implements JournalEntryRepositoryPort {
        private final List<JournalEntry> entries = new ArrayList<>();

        @Override
        public JournalEntry save(JournalEntry entry) {
            entries.add(entry);
            return entry;
        }

        @Override
        public boolean existsBySourceEventId(UUID sourceEventId) {
            return false;
        }

        @Override
        public List<JournalEntry> findByClinicId(UUID clinicId) {
            return entries.stream().filter(entry -> clinicId.equals(entry.getClinicId())).toList();
        }

        @Override
        public List<JournalEntry> findByClinicIdAndEntryDateBetween(UUID clinicId, LocalDate from, LocalDate to) {
            return entries.stream()
                    .filter(entry -> clinicId.equals(entry.getClinicId()))
                    .filter(entry -> !entry.getEntryDate().isBefore(from) && !entry.getEntryDate().isAfter(to))
                    .toList();
        }

        @Override
        public List<JournalEntry> findByClinicIdAndBankAccountId(UUID clinicId, UUID bankAccountId) {
            return List.of();
        }

        @Override
        public JournalQueryResult queryByClinic(
                UUID clinicId,
                LocalDate from,
                LocalDate to,
                String search,
                String sourceEventType,
                int page,
                int size) {
            List<JournalEntry> filtered = findByClinicId(clinicId).stream()
                    .filter(entry -> !entry.getEntryDate().isBefore(from) && !entry.getEntryDate().isAfter(to))
                    .filter(entry -> sourceEventType == null || sourceEventType.isBlank()
                            || sourceEventType.equals(entry.getSourceEventType()))
                    .toList();
            BigDecimal debit = filtered.stream()
                    .flatMap(entry -> entry.getLines().stream())
                    .map(line -> line.getDebit() == null ? BigDecimal.ZERO : line.getDebit())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal credit = filtered.stream()
                    .flatMap(entry -> entry.getLines().stream())
                    .map(line -> line.getCredit() == null ? BigDecimal.ZERO : line.getCredit())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            return new JournalQueryResult(filtered, filtered.size(), debit, credit, 0, size, 1);
        }
    }
}
