package com.jclinical.cash.domain.service;

import com.jclinical.cash.domain.model.CashExpense;
import com.jclinical.cash.domain.model.CashExpenseStatus;
import com.jclinical.cash.domain.model.CashSession;
import com.jclinical.cash.domain.ports.in.ManageCashExpensesUseCase;
import com.jclinical.cash.domain.ports.out.CashExpenseRepositoryPort;
import com.jclinical.cash.domain.ports.out.CashSessionRepositoryPort;
import com.jclinical.cash.domain.ports.out.CashStaffValidatorPort;
import com.jclinical.core.events.DomainEventPublisherPort;
import com.jclinical.core.events.DomainEventRoutingKeys;
import com.jclinical.core.events.CashExpenseRegisteredEvent;
import com.jclinical.core.events.CashExpenseVoidedEvent;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class CashExpenseService implements ManageCashExpensesUseCase {

    private final CashExpenseRepositoryPort expenseRepository;
    private final CashSessionRepositoryPort cashSessionRepository;
    private final CashStaffValidatorPort staffValidator;
    private final DomainEventPublisherPort eventPublisher;

    public CashExpenseService(
            CashExpenseRepositoryPort expenseRepository,
            CashSessionRepositoryPort cashSessionRepository,
            CashStaffValidatorPort staffValidator,
            DomainEventPublisherPort eventPublisher) {
        this.expenseRepository = expenseRepository;
        this.cashSessionRepository = cashSessionRepository;
        this.staffValidator = staffValidator;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public CashExpense registerExpense(UUID clinicId, RegisterExpenseCommand command) {
        staffValidator.findActiveStaff(command.createdByStaffId(), clinicId)
                .orElseThrow(() -> new IllegalArgumentException("El empleado indicado no existe o no está activo en esta clínica."));

        if (command.concept() == null || command.concept().isBlank()) {
            throw new IllegalArgumentException("El concepto del egreso es obligatorio.");
        }

        if (command.amount() == null || command.amount().signum() <= 0) {
            throw new IllegalArgumentException("El monto del egreso debe ser mayor a cero.");
        }

        CashSession session = cashSessionRepository.findOpenByClinicId(clinicId)
                .orElseThrow(() -> new IllegalStateException("No hay una caja abierta. Abre un turno antes de registrar egresos."));

        CashExpense expense = CashExpense.builder()
                .id(UUID.randomUUID())
                .clinicId(clinicId)
                .cashSessionId(session.getId())
                .concept(command.concept())
                .amount(command.amount())
                .createdByStaffId(command.createdByStaffId())
                .createdAt(LocalDateTime.now())
                .status(CashExpenseStatus.ACTIVE)
                .build();

        CashExpense saved = expenseRepository.save(expense);

        eventPublisher.publish(DomainEventRoutingKeys.CASH_EXPENSE_REGISTERED, new CashExpenseRegisteredEvent(
                UUID.randomUUID(),
                clinicId,
                saved.getId(),
                saved.getCashSessionId(),
                saved.getConcept(),
                saved.getAmount(),
                LocalDateTime.now()
        ));

        return saved;
    }

    @Override
    public CashExpense getExpense(UUID expenseId, UUID clinicId) {
        return expenseRepository.findByIdAndClinicId(expenseId, clinicId)
                .orElseThrow(() -> new IllegalArgumentException("El egreso no existe en esta clínica."));
    }

    @Override
    public List<CashExpense> listBySession(UUID cashSessionId, UUID clinicId) {
        return expenseRepository.findByCashSessionId(cashSessionId, clinicId);
    }

    @Override
    public CashExpense voidExpense(UUID expenseId, UUID clinicId, VoidExpenseCommand command) {
        CashExpense expense = getExpense(expenseId, clinicId);

        staffValidator.findActiveStaff(command.staffId(), clinicId)
                .orElseThrow(() -> new IllegalArgumentException("El empleado indicado no existe o no está activo en esta clínica."));

        expense.voidExpense(command.staffId(), command.reason());
        CashExpense saved = expenseRepository.save(expense);

        eventPublisher.publish(DomainEventRoutingKeys.CASH_EXPENSE_VOIDED, new CashExpenseVoidedEvent(
                UUID.randomUUID(),
                clinicId,
                saved.getId(),
                saved.getCashSessionId(),
                saved.getAmount(),
                command.reason(),
                LocalDateTime.now()
        ));

        return saved;
    }
}
