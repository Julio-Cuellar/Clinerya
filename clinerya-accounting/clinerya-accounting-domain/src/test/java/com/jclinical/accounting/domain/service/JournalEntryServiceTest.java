package com.jclinical.accounting.domain.service;

import com.jclinical.accounting.domain.model.JournalEntry;
import com.jclinical.accounting.domain.model.JournalLine;
import com.jclinical.accounting.domain.model.JournalQueryResult;
import com.jclinical.accounting.domain.ports.out.JournalEntryRepositoryPort;
import com.jclinical.core.events.CashExpenseRegisteredEvent;
import com.jclinical.core.events.CashExpenseVoidedEvent;
import com.jclinical.core.events.PaymentRegisteredEvent;
import com.jclinical.core.events.PayrollPaymentRegisteredEvent;
import com.jclinical.core.events.PurchaseOrderCreatedEvent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JournalEntryServiceTest {

    private final InMemoryJournalRepository repository = new InMemoryJournalRepository();
    private final JournalEntryService service = new JournalEntryService(repository);

    @Test
    void recordsMixedPaymentWithBankLineAndAdvanceWhenQuotationIsNotFullyPaid() {
        UUID bankAccountId = UUID.randomUUID();
        PaymentRegisteredEvent event = new PaymentRegisteredEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                new BigDecimal("250.00"),
                List.of(new PaymentRegisteredEvent.NonCashPaymentLine(bankAccountId, new BigDecimal("200.00"), "CARD", "AUTH-1")),
                false,
                LocalDateTime.parse("2026-08-09T10:15:00"));

        Optional<JournalEntry> result = service.recordPaymentRegistered(event);

        assertTrue(result.isPresent());
        JournalEntry entry = result.orElseThrow();
        assertEquals("PagoRegistrado", entry.getSourceEventType());
        assertEquals(LocalDate.parse("2026-08-09"), entry.getEntryDate());
        assertBalanced(entry, "350.00");
        assertLine(entry, "11100", new BigDecimal("100.00"), BigDecimal.ZERO, null);
        assertLine(entry, "11200", new BigDecimal("200.00"), BigDecimal.ZERO, bankAccountId);
        assertLine(entry, "11200", new BigDecimal("50.00"), BigDecimal.ZERO, null);
        assertLine(entry, "21100", BigDecimal.ZERO, new BigDecimal("350.00"), null);
    }

    @Test
    void skipsDuplicateSourceEvents() {
        UUID eventId = UUID.randomUUID();
        repository.knownSourceEventIds.add(eventId);
        PaymentRegisteredEvent event = new PaymentRegisteredEvent(
                eventId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                BigDecimal.TEN,
                BigDecimal.ZERO,
                List.of(),
                true,
                LocalDateTime.now());

        Optional<JournalEntry> result = service.recordPaymentRegistered(event);

        assertFalse(result.isPresent());
        assertEquals(0, repository.entries.size());
    }

    @Test
    void recordsCashExpenseAndVoidAsOppositeEntries() {
        UUID clinicId = UUID.randomUUID();
        CashExpenseRegisteredEvent expense = new CashExpenseRegisteredEvent(
                UUID.randomUUID(),
                clinicId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Papeleria",
                new BigDecimal("75.00"),
                LocalDateTime.parse("2026-08-09T13:00:00"));
        CashExpenseVoidedEvent voided = new CashExpenseVoidedEvent(
                UUID.randomUUID(),
                clinicId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("75.00"),
                "Captura duplicada",
                LocalDateTime.parse("2026-08-09T13:30:00"));

        JournalEntry expenseEntry = service.recordCashExpenseRegistered(expense).orElseThrow();
        JournalEntry voidEntry = service.recordCashExpenseVoided(voided).orElseThrow();

        assertLine(expenseEntry, "52200", new BigDecimal("75.00"), BigDecimal.ZERO, null);
        assertLine(expenseEntry, "11100", BigDecimal.ZERO, new BigDecimal("75.00"), null);
        assertLine(voidEntry, "11100", new BigDecimal("75.00"), BigDecimal.ZERO, null);
        assertLine(voidEntry, "52200", BigDecimal.ZERO, new BigDecimal("75.00"), null);
        assertBalanced(expenseEntry, "75.00");
        assertBalanced(voidEntry, "75.00");
    }

    @Test
    void recordsPurchaseOrderOnlyWhenBankAccountIsPresent() {
        PurchaseOrderCreatedEvent withoutBank = new PurchaseOrderCreatedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "PO-1",
                "Proveedor",
                new BigDecimal("900.00"),
                null,
                LocalDateTime.now());

        assertFalse(service.recordPurchaseOrderCreated(withoutBank).isPresent());

        UUID bankAccountId = UUID.randomUUID();
        PurchaseOrderCreatedEvent withBank = new PurchaseOrderCreatedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "PO-2",
                "Proveedor",
                new BigDecimal("900.00"),
                bankAccountId,
                LocalDateTime.parse("2026-08-09T09:00:00"));

        JournalEntry entry = service.recordPurchaseOrderCreated(withBank).orElseThrow();

        assertLine(entry, "12100", new BigDecimal("900.00"), BigDecimal.ZERO, null);
        assertLine(entry, "11200", BigDecimal.ZERO, new BigDecimal("900.00"), bankAccountId);
        assertBalanced(entry, "900.00");
    }

    @Test
    void rejectsPayrollWhenAmountsDoNotMatch() {
        PayrollPaymentRegisteredEvent invalid = new PayrollPaymentRegisteredEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                null,
                new BigDecimal("1000.00"),
                new BigDecimal("800.00"),
                new BigDecimal("100.00"),
                LocalDate.parse("2026-08-09"));

        assertThrows(IllegalArgumentException.class, () -> service.recordPayrollPayment(invalid));
    }

    @Test
    void createsManualEntryWithGeneratedIdsAndBalancedLines() {
        UUID clinicId = UUID.randomUUID();
        JournalEntry manual = JournalEntry.builder()
                .entryDate(LocalDate.parse("2026-08-09"))
                .description("Ajuste manual")
                .lines(List.of(
                        JournalLine.builder().accountCode("11100").debit(new BigDecimal("10.00")).credit(BigDecimal.ZERO).build(),
                        JournalLine.builder().accountCode("41000").debit(BigDecimal.ZERO).credit(new BigDecimal("10.00")).build()
                ))
                .build();

        JournalEntry saved = service.createManualEntry(clinicId, manual);

        assertEquals(clinicId, saved.getClinicId());
        assertEquals("Manual", saved.getSourceEventType());
        assertTrue(saved.getLines().stream().allMatch(line -> line.getId() != null));
        assertBalanced(saved, "10.00");
    }

    private static void assertBalanced(JournalEntry entry, String expectedTotal) {
        BigDecimal expected = new BigDecimal(expectedTotal);
        assertEquals(expected, entry.totalDebits());
        assertEquals(expected, entry.totalCredits());
    }

    private static void assertLine(
            JournalEntry entry,
            String accountCode,
            BigDecimal debit,
            BigDecimal credit,
            UUID bankAccountId) {
        boolean found = entry.getLines().stream().anyMatch(line ->
                accountCode.equals(line.getAccountCode())
                        && debit.compareTo(line.getDebit()) == 0
                        && credit.compareTo(line.getCredit()) == 0
                        && (bankAccountId == null || bankAccountId.equals(line.getBankAccountId())));
        assertTrue(found, "Expected line " + accountCode + " debit=" + debit + " credit=" + credit);
    }

    private static final class InMemoryJournalRepository implements JournalEntryRepositoryPort {
        private final List<JournalEntry> entries = new ArrayList<>();
        private final List<UUID> knownSourceEventIds = new ArrayList<>();

        @Override
        public JournalEntry save(JournalEntry entry) {
            entries.add(entry);
            knownSourceEventIds.add(entry.getSourceEventId());
            return entry;
        }

        @Override
        public boolean existsBySourceEventId(UUID sourceEventId) {
            return knownSourceEventIds.contains(sourceEventId);
        }

        @Override
        public List<JournalEntry> findByClinicId(UUID clinicId) {
            return entries.stream().filter(entry -> clinicId.equals(entry.getClinicId())).toList();
        }

        @Override
        public List<JournalEntry> findByClinicIdAndEntryDateBetween(UUID clinicId, LocalDate from, LocalDate to) {
            return findByClinicId(clinicId).stream()
                    .filter(entry -> !entry.getEntryDate().isBefore(from) && !entry.getEntryDate().isAfter(to))
                    .toList();
        }

        @Override
        public List<JournalEntry> findByClinicIdAndBankAccountId(UUID clinicId, UUID bankAccountId) {
            return findByClinicId(clinicId).stream()
                    .filter(entry -> entry.getLines().stream().anyMatch(line -> bankAccountId.equals(line.getBankAccountId())))
                    .toList();
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
            List<JournalEntry> filtered = findByClinicIdAndEntryDateBetween(clinicId, from, to);
            BigDecimal debit = filtered.stream().map(JournalEntry::totalDebits).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal credit = filtered.stream().map(JournalEntry::totalCredits).reduce(BigDecimal.ZERO, BigDecimal::add);
            return new JournalQueryResult(filtered, filtered.size(), debit, credit, page, size, 1);
        }
    }
}
