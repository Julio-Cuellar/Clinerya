package com.jclinical.accounting.domain.service;

import com.jclinical.accounting.domain.model.JournalEntry;
import com.jclinical.accounting.domain.model.JournalLine;
import com.jclinical.accounting.domain.model.JournalQueryResult;
import com.jclinical.accounting.domain.ports.in.ManageJournalUseCase;
import com.jclinical.accounting.domain.ports.out.JournalEntryRepositoryPort;
import com.jclinical.core.events.CashExpenseRegisteredEvent;
import com.jclinical.core.events.CashExpenseVoidedEvent;
import com.jclinical.core.events.ConsumoConciliadoEvent;
import com.jclinical.core.events.MermaCaducidadEvent;
import com.jclinical.core.events.PaymentRegisteredEvent;
import com.jclinical.core.events.PurchaseOrderCreatedEvent;
import com.jclinical.core.events.PayrollPaymentRegisteredEvent;
import com.jclinical.core.security.ClinicAccessDeniedException;
import com.jclinical.core.security.StaffPermission;
import com.jclinical.core.security.StaffPermissionCheckerPort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Locale;

public class JournalEntryService implements ManageJournalUseCase {

    private static final String COSTO_DIRECTO_SERVICIO = "51000";
    private static final String COSTO_DIRECTO_SERVICIO_NOMBRE = "Costo Directo del Servicio";
    private static final String ALMACEN_INSUMOS = "12100";
    private static final String ALMACEN_INSUMOS_NOMBRE = "Almacén de Insumos Clínicos";
    private static final String CAJA_OPERATIVA = "11100";
    private static final String CAJA_OPERATIVA_NOMBRE = "Caja Operativa";
    private static final String BANCOS = "11200";
    private static final String BANCOS_NOMBRE = "Bancos";
    private static final String INGRESOS_SERVICIOS = "41000";
    private static final String INGRESOS_SERVICIOS_NOMBRE = "Ingresos por Servicios Médicos";
    private static final String ANTICIPOS_PACIENTES = "21100";
    private static final String ANTICIPOS_PACIENTES_NOMBRE = "Anticipos de Pacientes";
    private static final String MERMA_CADUCIDAD = "52100";
    private static final String MERMA_CADUCIDAD_NOMBRE = "Merma y Caducidad";
    private static final String GASTOS_GENERALES = "52200";
    private static final String GASTOS_GENERALES_NOMBRE = "Gastos Generales de Caja";
    private static final String SUELDOS_SALARIOS = "52000";
    private static final String SUELDOS_SALARIOS_NOMBRE = "Sueldos y salarios";
    private static final String RETENCIONES_NOMINA = "21300";
    private static final String RETENCIONES_NOMINA_NOMBRE = "Retenciones y deducciones por pagar";

    private final JournalEntryRepositoryPort repository;
    private final StaffPermissionCheckerPort permissionChecker;

    public JournalEntryService(JournalEntryRepositoryPort repository, StaffPermissionCheckerPort permissionChecker) {
        this.repository = repository;
        this.permissionChecker = permissionChecker;
    }

    private void authorize(UUID actingUserId, UUID clinicId, StaffPermission permission) {
        if (actingUserId == null || !permissionChecker.hasPermission(clinicId, actingUserId, permission)) {
            throw new ClinicAccessDeniedException("No tienes permisos para esta operación de contabilidad.");
        }
    }

    @Override
    public Optional<JournalEntry> recordConsumptionReconciled(ConsumoConciliadoEvent event) {
        if (repository.existsBySourceEventId(event.eventId())) {
            return Optional.empty();
        }

        BigDecimal totalAmount = event.materials().stream()
                .map(line -> {
                    BigDecimal qty = line.actualQuantity() != null ? line.actualQuantity() : BigDecimal.ZERO;
                    BigDecimal cost = line.unitCostAtMovement() != null ? line.unitCostAtMovement() : BigDecimal.ZERO;
                    return qty.multiply(cost);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalAmount.signum() <= 0) {
            return Optional.empty();
        }

        JournalEntry entry = JournalEntry.builder()
                .id(UUID.randomUUID())
                .clinicId(event.clinicId())
                .description("Consumo de insumos conciliado - visita " + event.visitId())
                .entryDate(eventDate(event.occurredAt()))
                .sourceEventType("ConsumoConciliado")
                .sourceEventId(event.eventId())
                .createdAt(now())
                .lines(List.of(
                        JournalLine.builder()
                                .id(UUID.randomUUID())
                                .accountCode(COSTO_DIRECTO_SERVICIO)
                                .accountName(COSTO_DIRECTO_SERVICIO_NOMBRE)
                                .debit(totalAmount)
                                .credit(BigDecimal.ZERO)
                                .build(),
                        JournalLine.builder()
                                .id(UUID.randomUUID())
                                .accountCode(ALMACEN_INSUMOS)
                                .accountName(ALMACEN_INSUMOS_NOMBRE)
                                .debit(BigDecimal.ZERO)
                                .credit(totalAmount)
                                .build()
                ))
                .build();

        entry.validateBalanced();

        return Optional.of(repository.save(entry));
    }

    @Override
    public Optional<JournalEntry> recordPaymentRegistered(PaymentRegisteredEvent event) {
        if (repository.existsBySourceEventId(event.eventId())) {
            return Optional.empty();
        }

        BigDecimal cashAmount = event.cashAmount() != null ? event.cashAmount() : BigDecimal.ZERO;
        BigDecimal nonCashAmount = event.nonCashAmount() != null ? event.nonCashAmount() : BigDecimal.ZERO;
        BigDecimal totalAmount = cashAmount.add(nonCashAmount);

        if (totalAmount.signum() <= 0) {
            return Optional.empty();
        }

        List<JournalLine> lines = new ArrayList<>();

        if (cashAmount.signum() > 0) {
            lines.add(JournalLine.builder()
                    .id(UUID.randomUUID())
                    .accountCode(CAJA_OPERATIVA)
                    .accountName(CAJA_OPERATIVA_NOMBRE)
                    .debit(cashAmount)
                    .credit(BigDecimal.ZERO)
                    .build());
        }

        addNonCashPaymentLines(lines, event, nonCashAmount);

        String creditAccountCode = event.fullyPaid() ? INGRESOS_SERVICIOS : ANTICIPOS_PACIENTES;
        String creditAccountName = event.fullyPaid() ? INGRESOS_SERVICIOS_NOMBRE : ANTICIPOS_PACIENTES_NOMBRE;
        lines.add(JournalLine.builder()
                .id(UUID.randomUUID())
                .accountCode(creditAccountCode)
                .accountName(creditAccountName)
                .debit(BigDecimal.ZERO)
                .credit(totalAmount)
                .build());

        JournalEntry entry = JournalEntry.builder()
                .id(UUID.randomUUID())
                .clinicId(event.clinicId())
                .description("Cobro registrado en caja - ticket " + event.ticketId())
                .entryDate(eventDate(event.occurredAt()))
                .sourceEventType("PagoRegistrado")
                .sourceEventId(event.eventId())
                .createdAt(now())
                .lines(lines)
                .build();

        entry.validateBalanced();

        return Optional.of(repository.save(entry));
    }

    @Override
    public Optional<JournalEntry> recordSupplyWasted(MermaCaducidadEvent event) {
        if (repository.existsBySourceEventId(event.eventId())) {
            return Optional.empty();
        }

        BigDecimal quantity = event.quantity() != null ? event.quantity() : BigDecimal.ZERO;
        BigDecimal unitCost = event.unitCostAtMovement() != null ? event.unitCostAtMovement() : BigDecimal.ZERO;
        BigDecimal totalAmount = quantity.multiply(unitCost);

        if (totalAmount.signum() <= 0) {
            return Optional.empty();
        }

        String description = "Merma o caducidad de insumo" + (event.materialName() != null ? " - " + event.materialName() : "");

        JournalEntry entry = JournalEntry.builder()
                .id(UUID.randomUUID())
                .clinicId(event.clinicId())
                .description(description)
                .entryDate(eventDate(event.occurredAt()))
                .sourceEventType("MermaCaducidad")
                .sourceEventId(event.eventId())
                .createdAt(now())
                .lines(List.of(
                        JournalLine.builder()
                                .id(UUID.randomUUID())
                                .accountCode(MERMA_CADUCIDAD)
                                .accountName(MERMA_CADUCIDAD_NOMBRE)
                                .debit(totalAmount)
                                .credit(BigDecimal.ZERO)
                                .build(),
                        JournalLine.builder()
                                .id(UUID.randomUUID())
                                .accountCode(ALMACEN_INSUMOS)
                                .accountName(ALMACEN_INSUMOS_NOMBRE)
                                .debit(BigDecimal.ZERO)
                                .credit(totalAmount)
                                .build()
                ))
                .build();

        entry.validateBalanced();

        return Optional.of(repository.save(entry));
    }

    @Override
    public List<JournalEntry> listByClinic(UUID clinicId, UUID actingUserId) {
        authorize(actingUserId, clinicId, StaffPermission.VIEW_JOURNAL_ENTRIES);
        return repository.findByClinicId(clinicId);
    }

    @Override
    public JournalQueryResult queryByClinic(
            UUID clinicId,
            UUID actingUserId,
            LocalDate from,
            LocalDate to,
            String search,
            String sourceEventType,
            int page,
            int size) {
        authorize(actingUserId, clinicId, StaffPermission.VIEW_JOURNAL_ENTRIES);
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la fecha final.");
        }
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("La paginacion del diario no es valida.");
        }
        LocalDate normalizedFrom = from == null ? LocalDate.of(1900, 1, 1) : from;
        LocalDate normalizedTo = to == null ? LocalDate.of(9999, 12, 31) : to;
        String normalizedSearch = search == null ? "" : search.trim();
        String normalizedEvent = sourceEventType == null || "ALL".equalsIgnoreCase(sourceEventType)
                ? "" : sourceEventType.trim();
        return repository.queryByClinic(clinicId, normalizedFrom, normalizedTo, normalizedSearch, normalizedEvent, page, size);
    }

    @Override
    public JournalEntry createManualEntry(UUID clinicId, UUID actingUserId, JournalEntry entry) {
        authorize(actingUserId, clinicId, StaffPermission.CREATE_JOURNAL_ENTRIES);
        if (entry.getId() == null) {
            entry.setId(UUID.randomUUID());
        }
        entry.setClinicId(clinicId);
        entry.setSourceEventType("Manual");
        entry.setSourceEventId(UUID.randomUUID());
        entry.setCreatedAt(now());

        if (entry.getLines() != null) {
            entry.getLines().forEach(line -> {
                if (line.getId() == null) {
                    line.setId(UUID.randomUUID());
                }
            });
        }

        entry.validateBalanced();
        return repository.save(entry);
    }

    @Override
    public Optional<JournalEntry> recordCashExpenseRegistered(CashExpenseRegisteredEvent event) {
        if (repository.existsBySourceEventId(event.eventId())) {
            return Optional.empty();
        }

        BigDecimal amount = event.amount() != null ? event.amount() : BigDecimal.ZERO;
        if (amount.signum() <= 0) {
            return Optional.empty();
        }

        String description = "Egreso de caja: " + (event.concept() != null ? event.concept() : "sin concepto");

        JournalEntry entry = JournalEntry.builder()
                .id(UUID.randomUUID())
                .clinicId(event.clinicId())
                .description(description)
                .entryDate(eventDate(event.occurredAt()))
                .sourceEventType("EgresoCaja")
                .sourceEventId(event.eventId())
                .createdAt(now())
                .lines(List.of(
                        JournalLine.builder()
                                .id(UUID.randomUUID())
                                .accountCode(GASTOS_GENERALES)
                                .accountName(GASTOS_GENERALES_NOMBRE)
                                .debit(amount)
                                .credit(BigDecimal.ZERO)
                                .build(),
                        JournalLine.builder()
                                .id(UUID.randomUUID())
                                .accountCode(CAJA_OPERATIVA)
                                .accountName(CAJA_OPERATIVA_NOMBRE)
                                .debit(BigDecimal.ZERO)
                                .credit(amount)
                                .build()
                ))
                .build();

        entry.validateBalanced();

        return Optional.of(repository.save(entry));
    }

    @Override
    public Optional<JournalEntry> recordCashExpenseVoided(CashExpenseVoidedEvent event) {
        if (repository.existsBySourceEventId(event.eventId())) {
            return Optional.empty();
        }

        BigDecimal amount = event.amount() != null ? event.amount() : BigDecimal.ZERO;
        if (amount.signum() <= 0) {
            return Optional.empty();
        }

        String description = "Contra-póliza por anulación de egreso de caja (Razón: " 
                + (event.reason() != null ? event.reason() : "no especificada") + ")";

        JournalEntry entry = JournalEntry.builder()
                .id(UUID.randomUUID())
                .clinicId(event.clinicId())
                .description(description)
                .entryDate(eventDate(event.occurredAt()))
                .sourceEventType("EgresoCajaAnulado")
                .sourceEventId(event.eventId())
                .createdAt(now())
                .lines(List.of(
                        JournalLine.builder()
                                .id(UUID.randomUUID())
                                .accountCode(CAJA_OPERATIVA)
                                .accountName(CAJA_OPERATIVA_NOMBRE)
                                .debit(amount)
                                .credit(BigDecimal.ZERO)
                                .build(),
                        JournalLine.builder()
                                .id(UUID.randomUUID())
                                .accountCode(GASTOS_GENERALES)
                                .accountName(GASTOS_GENERALES_NOMBRE)
                                .debit(BigDecimal.ZERO)
                                .credit(amount)
                                .build()
                ))
                .build();

        entry.validateBalanced();

        return Optional.of(repository.save(entry));
    }

    private void addNonCashPaymentLines(List<JournalLine> lines, PaymentRegisteredEvent event, BigDecimal nonCashAmount) {
        List<PaymentRegisteredEvent.NonCashPaymentLine> nonCashLines = event.nonCashLines() != null
                ? event.nonCashLines()
                : List.of();
        BigDecimal assignedAmount = BigDecimal.ZERO;

        for (PaymentRegisteredEvent.NonCashPaymentLine line : nonCashLines) {
            if (line == null) {
                continue;
            }
            BigDecimal amount = line.amount() != null ? line.amount() : BigDecimal.ZERO;
            if (amount.signum() <= 0) {
                continue;
            }
            assignedAmount = assignedAmount.add(amount);
            lines.add(bankDebitLine(amount, line.bankAccountId()));
        }

        BigDecimal fallbackAmount = nonCashAmount.subtract(assignedAmount);
        if (fallbackAmount.signum() > 0) {
            lines.add(bankDebitLine(fallbackAmount, null));
        }
    }

    private JournalLine bankDebitLine(BigDecimal amount, UUID bankAccountId) {
        return JournalLine.builder()
                .id(UUID.randomUUID())
                .bankAccountId(bankAccountId)
                .accountCode(BANCOS)
                .accountName(BANCOS_NOMBRE)
                .debit(amount)
                .credit(BigDecimal.ZERO)
                .build();
    }

    @Override
    public Optional<JournalEntry> recordPurchaseOrderCreated(PurchaseOrderCreatedEvent event) {
        if (repository.existsBySourceEventId(event.eventId())) {
            return Optional.empty();
        }

        if (event.bankAccountId() == null) {
            return Optional.empty();
        }

        BigDecimal totalAmount = event.totalAmount();
        if (totalAmount.signum() <= 0) {
            return Optional.empty();
        }

        String description = "Pago de pedido de insumos: " + event.folio() + " - " + event.supplierName();

        JournalEntry entry = JournalEntry.builder()
                .id(UUID.randomUUID())
                .clinicId(event.clinicId())
                .description(description)
                .entryDate(eventDate(event.occurredAt()))
                .sourceEventType("PedidoInsumos")
                .sourceEventId(event.eventId())
                .createdAt(now())
                .lines(List.of(
                        JournalLine.builder()
                                .id(UUID.randomUUID())
                                .accountCode(ALMACEN_INSUMOS)
                                .accountName(ALMACEN_INSUMOS_NOMBRE)
                                .debit(totalAmount)
                                .credit(BigDecimal.ZERO)
                                .build(),
                        JournalLine.builder()
                                .id(UUID.randomUUID())
                                .bankAccountId(event.bankAccountId())
                                .accountCode(BANCOS)
                                .accountName(BANCOS_NOMBRE)
                                .debit(BigDecimal.ZERO)
                                .credit(totalAmount)
                                .build()
                ))
                .build();

        entry.validateBalanced();

        return Optional.of(repository.save(entry));
    }

    @Override
    public Optional<JournalEntry> recordPayrollPayment(PayrollPaymentRegisteredEvent event) {
        if (repository.existsBySourceEventId(event.eventId())) {
            return Optional.empty();
        }

        BigDecimal grossAmount = amountOrZero(event.grossAmount());
        BigDecimal netAmount = amountOrZero(event.netAmount());
        BigDecimal deductionAmount = amountOrZero(event.deductionAmount());
        if (grossAmount.signum() <= 0 || netAmount.signum() <= 0 || deductionAmount.signum() < 0
                || grossAmount.compareTo(netAmount.add(deductionAmount)) != 0
                || event.bankAccountId() == null) {
            throw new IllegalArgumentException("Los importes del pago de nomina no son validos.");
        }

        List<JournalLine> lines = new ArrayList<>();
        lines.add(JournalLine.builder()
                .id(UUID.randomUUID())
                .accountCode(SUELDOS_SALARIOS)
                .accountName(SUELDOS_SALARIOS_NOMBRE)
                .debit(grossAmount)
                .credit(BigDecimal.ZERO)
                .build());
        lines.add(JournalLine.builder()
                .id(UUID.randomUUID())
                .bankAccountId(event.bankAccountId())
                .accountCode(event.fundingAccountCode() != null ? event.fundingAccountCode() : BANCOS)
                .accountName(event.fundingAccountName() != null ? event.fundingAccountName() : BANCOS_NOMBRE)
                .debit(BigDecimal.ZERO)
                .credit(netAmount)
                .build());
        if (deductionAmount.signum() > 0) {
            lines.add(JournalLine.builder()
                    .id(UUID.randomUUID())
                    .accountCode(RETENCIONES_NOMINA)
                    .accountName(RETENCIONES_NOMINA_NOMBRE)
                    .debit(BigDecimal.ZERO)
                    .credit(deductionAmount)
                    .build());
        }

        JournalEntry entry = JournalEntry.builder()
                .id(UUID.randomUUID())
                .clinicId(event.clinicId())
                .description("Pago de nomina - periodo " + event.payrollPeriodId())
                .entryDate(event.paymentDate() != null ? event.paymentDate() : LocalDate.now())
                .sourceEventType("PagoNomina")
                .sourceEventId(event.eventId())
                .createdAt(now())
                .lines(lines)
                .build();

        entry.validateBalanced();
        return Optional.of(repository.save(entry));
    }

    private BigDecimal amountOrZero(BigDecimal amount) {
        return amount != null ? amount : BigDecimal.ZERO;
    }

    private LocalDate eventDate(LocalDateTime occurredAt) {
        return occurredAt != null ? occurredAt.toLocalDate() : LocalDate.now();
    }

    private static LocalDateTime now() {
        return LocalDateTime.now(ZoneId.systemDefault());
    }
}
