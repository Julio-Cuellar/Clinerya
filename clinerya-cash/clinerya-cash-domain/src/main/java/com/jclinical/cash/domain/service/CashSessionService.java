package com.jclinical.cash.domain.service;

import com.jclinical.cash.domain.model.CashSession;
import com.jclinical.cash.domain.model.CashSessionStatus;
import com.jclinical.cash.domain.ports.in.ManageCashSessionUseCase;
import com.jclinical.cash.domain.ports.out.CashExpenseRepositoryPort;
import com.jclinical.cash.domain.ports.out.CashSessionRepositoryPort;
import com.jclinical.cash.domain.ports.out.CashStaffValidatorPort;
import com.jclinical.cash.domain.ports.out.TicketRepositoryPort;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class CashSessionService implements ManageCashSessionUseCase {

    private final CashSessionRepositoryPort cashSessionRepository;
    private final TicketRepositoryPort ticketRepository;
    private final CashStaffValidatorPort staffValidator;
    private final CashExpenseRepositoryPort expenseRepository;

    public CashSessionService(
            CashSessionRepositoryPort cashSessionRepository,
            TicketRepositoryPort ticketRepository,
            CashStaffValidatorPort staffValidator,
            CashExpenseRepositoryPort expenseRepository) {
        this.cashSessionRepository = cashSessionRepository;
        this.ticketRepository = ticketRepository;
        this.staffValidator = staffValidator;
        this.expenseRepository = expenseRepository;
    }

    @Override
    public CashSession openSession(UUID clinicId, OpenSessionCommand command) {
        if (command.openingAmount() == null || command.openingAmount().signum() < 0) {
            throw new IllegalArgumentException("El monto de apertura no puede ser negativo.");
        }

        staffValidator.findActiveStaff(command.openedByStaffId(), clinicId)
                .orElseThrow(() -> new IllegalArgumentException("El empleado indicado no existe o no está activo en esta clínica."));

        if (cashSessionRepository.findOpenByClinicId(clinicId).isPresent()) {
            throw new IllegalStateException(
                    "Ya existe un turno de caja abierto para esta clínica. Ciérralo antes de abrir uno nuevo.");
        }

        CashSession session = CashSession.builder()
                .id(UUID.randomUUID())
                .clinicId(clinicId)
                .openedByStaffId(command.openedByStaffId())
                .openedAt(LocalDateTime.now())
                .openingAmount(command.openingAmount())
                .status(CashSessionStatus.OPEN)
                .build();

        return cashSessionRepository.save(session);
    }

    @Override
    public CashSession closeSession(UUID clinicId, CloseSessionCommand command) {
        CashSession session = cashSessionRepository.findOpenByClinicIdForUpdate(clinicId)
                .orElseThrow(() -> new IllegalStateException("No hay un turno de caja abierto para cerrar."));

        staffValidator.findActiveStaff(command.closedByStaffId(), clinicId)
                .orElseThrow(() -> new IllegalArgumentException("El empleado indicado no existe o no está activo en esta clínica."));

        if (command.countedCashAmount() == null || command.countedCashAmount().signum() < 0) {
            throw new IllegalArgumentException("El monto contado no puede ser negativo.");
        }

        BigDecimal expectedCashAmount = session.getOpeningAmount()
                .add(ticketRepository.sumActiveCashAmountBySession(session.getId()))
                .subtract(expenseRepository.sumActiveAmountBySession(session.getId()));

        session.close(command.countedCashAmount(), expectedCashAmount, command.closedByStaffId());

        return cashSessionRepository.save(session);
    }

    @Override
    public Optional<CashSession> getCurrentSession(UUID clinicId) {
        return cashSessionRepository.findOpenByClinicId(clinicId);
    }

    @Override
    public CashSession getSession(UUID sessionId, UUID clinicId) {
        return cashSessionRepository.findByIdAndClinicId(sessionId, clinicId)
                .orElseThrow(() -> new IllegalArgumentException("El turno de caja no existe en esta clínica."));
    }

    @Override
    public List<CashSession> listSessions(UUID clinicId) {
        return cashSessionRepository.findByClinicId(clinicId);
    }
}
