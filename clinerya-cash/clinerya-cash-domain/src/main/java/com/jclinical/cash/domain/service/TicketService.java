package com.jclinical.cash.domain.service;

import com.jclinical.cash.domain.model.CashSession;
import com.jclinical.cash.domain.model.PaymentLine;
import com.jclinical.cash.domain.model.PaymentMethod;
import com.jclinical.cash.domain.model.Ticket;
import com.jclinical.cash.domain.model.TicketStatus;
import com.jclinical.cash.domain.ports.in.ManageTicketsUseCase;
import com.jclinical.cash.domain.ports.out.CashBankAccountValidatorPort;
import com.jclinical.cash.domain.ports.out.CashPatientValidatorPort;
import com.jclinical.cash.domain.ports.out.CashQuotationValidatorPort;
import com.jclinical.cash.domain.ports.out.CashQuotationValidatorPort.QuotationSnapshot;
import com.jclinical.cash.domain.ports.out.CashSessionRepositoryPort;
import com.jclinical.cash.domain.ports.out.CashStaffValidatorPort;
import com.jclinical.cash.domain.ports.out.TicketRepositoryPort;
import com.jclinical.core.events.DomainEventPublisherPort;
import com.jclinical.core.events.DomainEventRoutingKeys;
import com.jclinical.core.events.PaymentRegisteredEvent;
import com.jclinical.core.security.ClinicAccessDeniedException;
import com.jclinical.core.security.StaffPermission;
import com.jclinical.core.security.StaffPermissionCheckerPort;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class TicketService implements ManageTicketsUseCase {

    private final TicketRepositoryPort ticketRepository;
    private final CashSessionRepositoryPort cashSessionRepository;
    private final CashPatientValidatorPort patientValidator;
    private final CashQuotationValidatorPort quotationValidator;
    private final CashStaffValidatorPort staffValidator;
    private final CashBankAccountValidatorPort bankAccountValidator;
    private final DomainEventPublisherPort eventPublisher;
    private final StaffPermissionCheckerPort permissionChecker;

    public TicketService(
            TicketRepositoryPort ticketRepository,
            CashSessionRepositoryPort cashSessionRepository,
            CashPatientValidatorPort patientValidator,
            CashQuotationValidatorPort quotationValidator,
            CashStaffValidatorPort staffValidator,
            CashBankAccountValidatorPort bankAccountValidator,
            DomainEventPublisherPort eventPublisher,
            StaffPermissionCheckerPort permissionChecker) {
        this.ticketRepository = ticketRepository;
        this.cashSessionRepository = cashSessionRepository;
        this.patientValidator = patientValidator;
        this.quotationValidator = quotationValidator;
        this.staffValidator = staffValidator;
        this.bankAccountValidator = bankAccountValidator;
        this.eventPublisher = eventPublisher;
        this.permissionChecker = permissionChecker;
    }

    @Override
    public Ticket registerTicket(UUID actingUserId, UUID clinicId, RegisterTicketCommand command) {
        requirePermission(clinicId, actingUserId, StaffPermission.CREATE_CHARGES,
                "No tienes permiso para registrar cobros en esta clinica.");
        if (!patientValidator.existsByIdAndClinicId(command.patientId(), clinicId)) {
            throw new IllegalArgumentException("El paciente no existe en esta clinica.");
        }

        if (command.createdByStaffId() == null) {
            throw new IllegalArgumentException("El ticket debe indicar el empleado responsable.");
        }

        staffValidator.findActiveStaff(command.createdByStaffId(), clinicId)
                .orElseThrow(() -> new IllegalArgumentException("El empleado indicado no existe o no esta activo en esta clinica."));

        CashSession session = cashSessionRepository.findOpenByClinicId(clinicId)
                .orElseThrow(() -> new IllegalStateException("No hay una caja abierta. Abre un turno antes de registrar cobros."));

        validatePaymentLines(command.paymentLines(), clinicId);

        BigDecimal totalAmount = command.paymentLines().stream()
                .map(PaymentLineCommand::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discountAmount = command.discountAmount() != null ? command.discountAmount() : BigDecimal.ZERO;
        if (discountAmount.signum() < 0) {
            throw new IllegalArgumentException("El monto del descuento no puede ser negativo.");
        }
        if (discountAmount.signum() > 0) {
            if (command.discountReason() == null || command.discountReason().isBlank()) {
                throw new IllegalArgumentException("Todo descuento debe indicar una razon.");
            }
            if (command.discountAuthorizedByStaffId() == null) {
                throw new IllegalArgumentException("Todo descuento debe ser autorizado por un empleado.");
            }
            staffValidator.findActiveStaff(command.discountAuthorizedByStaffId(), clinicId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "El empleado que autoriza el descuento no existe o no esta activo en esta clinica."));
        }

        BigDecimal settledAmount = totalAmount.add(discountAmount);
        boolean fullyPaid = true;

        if (command.quotationId() != null) {
            QuotationSnapshot snapshot = quotationValidator.findQuotation(command.quotationId(), command.patientId(), clinicId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "La cotizacion indicada no existe o no pertenece a este paciente."));

            if (!snapshot.accepted()) {
                throw new IllegalArgumentException("Solo se pueden registrar cobros contra una cotizacion aceptada.");
            }

            BigDecimal alreadySettled = ticketRepository.sumActiveAmountByQuotation(command.quotationId(), clinicId);
            BigDecimal remainingBalance = snapshot.grandTotal().subtract(alreadySettled);

            if (settledAmount.compareTo(remainingBalance) > 0) {
                throw new IllegalArgumentException(
                        "El monto del ticket ($" + settledAmount + ") excede el saldo pendiente de la cotizacion ($" + remainingBalance + ").");
            }

            // Si tras este ticket ya no queda saldo pendiente de la cotizacion, el ingreso se
            // reconoce por completo; si aun queda saldo, este cobro se registra como anticipo.
            fullyPaid = remainingBalance.subtract(settledAmount).signum() <= 0;
        }

        Integer folio = ticketRepository.nextFolio(clinicId);

        List<PaymentLine> paymentLines = command.paymentLines().stream()
                .map(lineCommand -> PaymentLine.builder()
                        .id(UUID.randomUUID())
                        .method(lineCommand.method())
                        .amount(lineCommand.amount())
                        .reference(lineCommand.reference())
                        .bankAccountId(lineCommand.bankAccountId())
                        .build())
                .toList();

        Ticket ticket = Ticket.builder()
                .id(UUID.randomUUID())
                .clinicId(clinicId)
                .cashSessionId(session.getId())
                .patientId(command.patientId())
                .quotationId(command.quotationId())
                .folio(folio)
                .totalAmount(totalAmount)
                .concept(command.concept())
                .createdByStaffId(command.createdByStaffId())
                .createdAt(LocalDateTime.now())
                .status(TicketStatus.ACTIVE)
                .discountAmount(discountAmount)
                .discountAuthorizedByStaffId(command.discountAuthorizedByStaffId())
                .discountReason(command.discountReason())
                .paymentLines(new java.util.ArrayList<>(paymentLines))
                .build();

        Ticket saved = ticketRepository.save(ticket);

        BigDecimal cashAmount = paymentLines.stream()
                .filter(line -> line.getMethod() == PaymentMethod.CASH)
                .map(PaymentLine::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal nonCashAmount = totalAmount.subtract(cashAmount);
        List<PaymentRegisteredEvent.NonCashPaymentLine> nonCashLines = paymentLines.stream()
                .filter(line -> line.getMethod() != PaymentMethod.CASH)
                .map(line -> new PaymentRegisteredEvent.NonCashPaymentLine(
                        line.getBankAccountId(),
                        line.getAmount(),
                        line.getMethod().name(),
                        line.getReference()
                ))
                .toList();

        eventPublisher.publish(DomainEventRoutingKeys.PAYMENT_REGISTERED, new PaymentRegisteredEvent(
                UUID.randomUUID(),
                clinicId,
                command.patientId(),
                saved.getId(),
                session.getId(),
                command.quotationId(),
                cashAmount,
                nonCashAmount,
                nonCashLines,
                fullyPaid,
                LocalDateTime.now()
        ));

        return saved;
    }

    @Override
    public Ticket getTicket(UUID actingUserId, UUID ticketId, UUID clinicId) {
        requireCashRead(clinicId, actingUserId);
        return ticketRepository.findByIdAndClinicId(ticketId, clinicId)
                .orElseThrow(() -> new IllegalArgumentException("El ticket no existe en esta clinica."));
    }

    @Override
    public List<Ticket> listBySession(UUID actingUserId, UUID cashSessionId, UUID clinicId) {
        requireCashRead(clinicId, actingUserId);
        return ticketRepository.findByCashSessionId(cashSessionId, clinicId);
    }

    @Override
    public List<Ticket> listByClinicRange(UUID actingUserId, UUID clinicId, LocalDateTime from, LocalDateTime to) {
        requireCashRead(clinicId, actingUserId);
        if (from == null || to == null || !from.isBefore(to)) {
            throw new IllegalArgumentException("El rango de fechas es invalido.");
        }
        return ticketRepository.findByClinicIdAndRange(clinicId, from, to);
    }

    @Override
    public List<Ticket> listByQuotation(UUID actingUserId, UUID quotationId, UUID clinicId) {
        requireCashRead(clinicId, actingUserId);
        return ticketRepository.findByQuotationIdAndClinicId(quotationId, clinicId);
    }

    @Override
    public List<Ticket> listByPatient(UUID actingUserId, UUID patientId, UUID clinicId) {
        requireCashRead(clinicId, actingUserId);
        return ticketRepository.findByPatientIdAndClinicId(patientId, clinicId);
    }

    @Override
    public Ticket voidTicket(UUID actingUserId, UUID ticketId, UUID clinicId, VoidTicketCommand command) {
        requirePermission(clinicId, actingUserId, StaffPermission.MANAGE_REFUNDS,
                "No tienes permiso para cancelar cobros en esta clinica.");
        Ticket ticket = ticketRepository.findByIdAndClinicId(ticketId, clinicId)
                .orElseThrow(() -> new IllegalArgumentException("El ticket no existe en esta clinica."));

        staffValidator.findActiveStaff(command.staffId(), clinicId)
                .orElseThrow(() -> new IllegalArgumentException("El empleado indicado no existe o no esta activo en esta clinica."));

        ticket.voidTicket(command.staffId(), command.reason());
        return ticketRepository.save(ticket);
    }

    @Override
    public QuotationBalance getQuotationBalance(UUID actingUserId, UUID quotationId, UUID patientId, UUID clinicId) {
        requireCashRead(clinicId, actingUserId);
        QuotationSnapshot snapshot = quotationValidator.findQuotation(quotationId, patientId, clinicId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La cotizacion indicada no existe o no pertenece a este paciente."));

        BigDecimal paidAmount = ticketRepository.sumActiveAmountByQuotation(quotationId, clinicId);
        BigDecimal remainingBalance = snapshot.grandTotal().subtract(paidAmount);

        return new QuotationBalance(quotationId, snapshot.grandTotal(), paidAmount, remainingBalance);
    }

    private void requireCashRead(UUID clinicId, UUID actingUserId) {
        requirePermission(clinicId, actingUserId, StaffPermission.VIEW_CASH,
                "No tienes permiso para consultar la caja de esta clinica.");
    }

    private void requirePermission(UUID clinicId, UUID actingUserId, StaffPermission permission, String deniedMessage) {
        if (actingUserId == null) {
            throw new ClinicAccessDeniedException("Usuario no autenticado.");
        }
        if (!permissionChecker.hasPermission(clinicId, actingUserId, permission)) {
            throw new ClinicAccessDeniedException(deniedMessage);
        }
    }

    private void validatePaymentLines(List<PaymentLineCommand> paymentLines, UUID clinicId) {
        if (paymentLines == null || paymentLines.isEmpty()) {
            throw new IllegalArgumentException("El ticket debe incluir al menos una linea de pago.");
        }

        for (PaymentLineCommand lineCommand : paymentLines) {
            if (lineCommand.amount() == null || lineCommand.amount().signum() <= 0) {
                throw new IllegalArgumentException("El monto de cada linea de pago debe ser mayor a cero.");
            }
            if (lineCommand.method() == null) {
                throw new IllegalArgumentException("Cada linea de pago debe indicar una forma de pago.");
            }
            if (lineCommand.method() == PaymentMethod.CASH) {
                if (lineCommand.bankAccountId() != null) {
                    throw new IllegalArgumentException("Los pagos en efectivo no deben indicar cuenta bancaria.");
                }
            } else {
                if (lineCommand.bankAccountId() == null) {
                    throw new IllegalArgumentException("Selecciona la cuenta bancaria destino para cada pago no efectivo.");
                }
                bankAccountValidator.findActiveDebitAccount(lineCommand.bankAccountId(), clinicId)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "La cuenta bancaria destino no existe, no esta activa o no admite pagos de pacientes."));
            }
        }
    }
}
