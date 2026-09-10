package com.jclinical.cash.domain.service;

import com.jclinical.cash.domain.model.CashExpense;
import com.jclinical.cash.domain.model.CashExpenseStatus;
import com.jclinical.cash.domain.model.CashSession;
import com.jclinical.cash.domain.model.CashSessionStatus;
import com.jclinical.cash.domain.model.Ticket;
import com.jclinical.cash.domain.ports.out.CashExpenseRepositoryPort;
import com.jclinical.cash.domain.ports.out.CashSessionRepositoryPort;
import com.jclinical.cash.domain.ports.out.CashStaffValidatorPort;
import com.jclinical.cash.domain.ports.out.TicketRepositoryPort;
import com.jclinical.core.events.CashExpenseRegisteredEvent;
import com.jclinical.core.events.CashExpenseVoidedEvent;
import com.jclinical.core.events.DomainEventPublisherPort;
import com.jclinical.core.events.DomainEventRoutingKeys;
import com.jclinical.core.security.StaffPermission;
import com.jclinical.core.security.StaffPermissionCheckerPort;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CashSessionAndExpenseServiceTest {

    private final InMemoryCashSessionRepository sessionRepository = new InMemoryCashSessionRepository();
    private final InMemoryTicketRepository ticketRepository = new InMemoryTicketRepository();
    private final InMemoryCashExpenseRepository expenseRepository = new InMemoryCashExpenseRepository();
    private final StubStaffValidator staffValidator = new StubStaffValidator();
    private final RecordingPublisher publisher = new RecordingPublisher();
    private final StaffPermissionCheckerPort permissionChecker = (clinicId, userId, permission) -> true;
    private final UUID actingUserId = UUID.randomUUID();

    private final CashSessionService sessionService = new CashSessionService(
            sessionRepository,
            ticketRepository,
            staffValidator,
            expenseRepository,
            permissionChecker);
    private final CashExpenseService expenseService = new CashExpenseService(
            expenseRepository,
            sessionRepository,
            staffValidator,
            publisher,
            permissionChecker);

    @Test
    void opensAndClosesSessionUsingTicketCashAndExpenses() {
        UUID clinicId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        staffValidator.activeStaffId = staffId;

        CashSession opened = sessionService.openSession(
                clinicId,
                actingUserId,
                new CashSessionService.OpenSessionCommand(staffId, new BigDecimal("100.00")));
        ticketRepository.activeCashBySession = new BigDecimal("250.00");
        expenseRepository.activeAmountBySession = new BigDecimal("40.00");

        CashSession closed = sessionService.closeSession(
                clinicId,
                actingUserId,
                new CashSessionService.CloseSessionCommand(staffId, new BigDecimal("320.00")));

        assertEquals(opened.getId(), closed.getId());
        assertEquals(CashSessionStatus.CLOSED, closed.getStatus());
        assertEquals(new BigDecimal("310.00"), closed.getExpectedCashAmount());
        assertEquals(new BigDecimal("10.00"), closed.getCashDifference());
    }

    @Test
    void preventsOpeningSecondSessionForClinic() {
        UUID clinicId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        staffValidator.activeStaffId = staffId;
        sessionService.openSession(clinicId, actingUserId, new CashSessionService.OpenSessionCommand(staffId, BigDecimal.ZERO));

        CashSessionService.OpenSessionCommand command = new CashSessionService.OpenSessionCommand(staffId, BigDecimal.ZERO);
        assertThrows(IllegalStateException.class,
                () -> sessionService.openSession(clinicId, actingUserId, command));
    }

    @Test
    void rejectsSessionOperationsForInactiveStaff() {
        UUID clinicId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();

        CashSessionService.OpenSessionCommand command = new CashSessionService.OpenSessionCommand(staffId, BigDecimal.ZERO);
        assertThrows(IllegalArgumentException.class,
                () -> sessionService.openSession(clinicId, actingUserId, command));
    }

    @Test
    void registersCashExpenseAndPublishesDomainEvent() {
        UUID clinicId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        staffValidator.activeStaffId = staffId;
        sessionService.openSession(clinicId, actingUserId, new CashSessionService.OpenSessionCommand(staffId, BigDecimal.ZERO));

        CashExpense expense = expenseService.registerExpense(
                clinicId,
                actingUserId,
                new CashExpenseService.RegisterExpenseCommand("Gasolina", new BigDecimal("120.00"), staffId));

        assertEquals(CashExpenseStatus.ACTIVE, expense.getStatus());
        assertEquals(1, publisher.events.size());
        PublishedEvent event = publisher.events.get(0);
        assertEquals(DomainEventRoutingKeys.CASH_EXPENSE_REGISTERED, event.routingKey());
        CashExpenseRegisteredEvent payload = assertInstanceOf(CashExpenseRegisteredEvent.class, event.payload());
        assertEquals(expense.getId(), payload.cashExpenseId());
        assertEquals(new BigDecimal("120.00"), payload.amount());
    }

    @Test
    void voidsExpenseAndPublishesDomainEvent() {
        UUID clinicId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        staffValidator.activeStaffId = staffId;
        sessionService.openSession(clinicId, actingUserId, new CashSessionService.OpenSessionCommand(staffId, BigDecimal.ZERO));
        CashExpense expense = expenseService.registerExpense(
                clinicId,
                actingUserId,
                new CashExpenseService.RegisterExpenseCommand("Gasolina", new BigDecimal("120.00"), staffId));

        CashExpense voided = expenseService.voidExpense(
                expense.getId(),
                clinicId,
                actingUserId,
                new CashExpenseService.VoidExpenseCommand(staffId, "Duplicado"));

        assertEquals(CashExpenseStatus.VOIDED, voided.getStatus());
        assertEquals(2, publisher.events.size());
        PublishedEvent event = publisher.events.get(1);
        assertEquals(DomainEventRoutingKeys.CASH_EXPENSE_VOIDED, event.routingKey());
        CashExpenseVoidedEvent payload = assertInstanceOf(CashExpenseVoidedEvent.class, event.payload());
        assertEquals(expense.getId(), payload.cashExpenseId());
        assertEquals("Duplicado", payload.reason());
    }

    private static final class InMemoryCashSessionRepository implements CashSessionRepositoryPort {
        private final List<CashSession> sessions = new ArrayList<>();

        @Override
        public CashSession save(CashSession session) {
            sessions.removeIf(existing -> existing.getId().equals(session.getId()));
            sessions.add(session);
            return session;
        }

        @Override
        public Optional<CashSession> findOpenByClinicId(UUID clinicId) {
            return sessions.stream()
                    .filter(session -> clinicId.equals(session.getClinicId()))
                    .filter(session -> session.getStatus() == CashSessionStatus.OPEN)
                    .findFirst();
        }

        @Override
        public Optional<CashSession> findOpenByClinicIdForUpdate(UUID clinicId) {
            return findOpenByClinicId(clinicId);
        }

        @Override
        public Optional<CashSession> findByIdAndClinicId(UUID sessionId, UUID clinicId) {
            return sessions.stream()
                    .filter(session -> sessionId.equals(session.getId()) && clinicId.equals(session.getClinicId()))
                    .findFirst();
        }

        @Override
        public List<CashSession> findByClinicId(UUID clinicId) {
            return sessions.stream().filter(session -> clinicId.equals(session.getClinicId())).toList();
        }
    }

    private static final class InMemoryCashExpenseRepository implements CashExpenseRepositoryPort {
        private final List<CashExpense> expenses = new ArrayList<>();
        private BigDecimal activeAmountBySession = BigDecimal.ZERO;

        @Override
        public CashExpense save(CashExpense expense) {
            expenses.removeIf(existing -> existing.getId().equals(expense.getId()));
            expenses.add(expense);
            return expense;
        }

        @Override
        public Optional<CashExpense> findByIdAndClinicId(UUID expenseId, UUID clinicId) {
            return expenses.stream()
                    .filter(expense -> expenseId.equals(expense.getId()) && clinicId.equals(expense.getClinicId()))
                    .findFirst();
        }

        @Override
        public List<CashExpense> findByCashSessionId(UUID cashSessionId, UUID clinicId) {
            return expenses.stream()
                    .filter(expense -> cashSessionId.equals(expense.getCashSessionId()) && clinicId.equals(expense.getClinicId()))
                    .toList();
        }

        @Override
        public BigDecimal sumActiveAmountBySession(UUID cashSessionId) {
            return activeAmountBySession;
        }
    }

    private static final class InMemoryTicketRepository implements TicketRepositoryPort {
        private BigDecimal activeCashBySession = BigDecimal.ZERO;

        @Override
        public Ticket save(Ticket ticket) {
            return ticket;
        }

        @Override
        public Optional<Ticket> findByIdAndClinicId(UUID ticketId, UUID clinicId) {
            return Optional.empty();
        }

        @Override
        public List<Ticket> findByCashSessionId(UUID cashSessionId, UUID clinicId) {
            return List.of();
        }

        @Override
        public List<Ticket> findByClinicIdAndRange(UUID clinicId, LocalDateTime from, LocalDateTime to) {
            return List.of();
        }

        @Override
        public List<Ticket> findByQuotationIdAndClinicId(UUID quotationId, UUID clinicId) {
            return List.of();
        }

        @Override
        public List<Ticket> findByPatientIdAndClinicId(UUID patientId, UUID clinicId) {
            return List.of();
        }

        @Override
        public BigDecimal sumActiveCashAmountBySession(UUID cashSessionId) {
            return activeCashBySession;
        }

        @Override
        public BigDecimal sumActiveAmountByQuotation(UUID quotationId, UUID clinicId) {
            return BigDecimal.ZERO;
        }

        @Override
        public Integer nextFolio(UUID clinicId) {
            return 1;
        }
    }

    private static final class StubStaffValidator implements CashStaffValidatorPort {
        private UUID activeStaffId;

        @Override
        public Optional<StaffSnapshot> findActiveStaff(UUID staffId, UUID clinicId) {
            if (staffId != null && staffId.equals(activeStaffId)) {
                return Optional.of(new StaffSnapshot(staffId, "Cajero"));
            }
            return Optional.empty();
        }
    }

    private static final class RecordingPublisher implements DomainEventPublisherPort {
        private final List<PublishedEvent> events = new ArrayList<>();

        @Override
        public void publish(String routingKey, Object payload) {
            events.add(new PublishedEvent(routingKey, payload));
        }
    }

    private record PublishedEvent(String routingKey, Object payload) {
    }
}
