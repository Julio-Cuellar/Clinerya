package com.jclinical.accounting.domain.ports.in;

import com.jclinical.accounting.domain.model.JournalEntry;
import com.jclinical.accounting.domain.model.JournalQueryResult;
import com.jclinical.core.events.CashExpenseRegisteredEvent;
import com.jclinical.core.events.CashExpenseVoidedEvent;
import com.jclinical.core.events.ConsumoConciliadoEvent;
import com.jclinical.core.events.MermaCaducidadEvent;
import com.jclinical.core.events.PaymentRegisteredEvent;
import com.jclinical.core.events.PayrollPaymentRegisteredEvent;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.LocalDate;

public interface ManageJournalUseCase {

    /**
     * Momento 2 del ARE: consumo real de insumos conciliado en una visita.
     * Débito 51000 Costo Directo del Servicio / Crédito 12100 Almacén de Insumos Clínicos.
     * Idempotente por sourceEventId — si ya se procesó este evento, no crea una póliza duplicada.
     */
    Optional<JournalEntry> recordConsumptionReconciled(ConsumoConciliadoEvent event);

    /**
     * Momento 1 del ARE: cobro registrado en Caja.
     * Débito Caja Operativa (11100) y/o Bancos (11200) según la forma de pago;
     * Crédito Ingresos por Servicios Médicos (41000) si el pago liquida por completo lo debido,
     * o Anticipos de Pacientes (21100) si aún queda saldo pendiente de la cotización.
     * Idempotente por sourceEventId.
     */
    Optional<JournalEntry> recordPaymentRegistered(PaymentRegisteredEvent event);

    /**
     * Momento 3 del ARE: ajuste de inventario por merma o caducidad.
     * Débito 52100 Merma y Caducidad / Crédito 12100 Almacén de Insumos Clínicos.
     * Idempotente por sourceEventId.
     */
    Optional<JournalEntry> recordSupplyWasted(MermaCaducidadEvent event);

    List<JournalEntry> listByClinic(UUID actingUserId, UUID clinicId);

    JournalQueryResult queryByClinic(
            UUID actingUserId,
            UUID clinicId,
            LocalDate from,
            LocalDate to,
            String search,
            String sourceEventType,
            int page,
            int size);

    JournalEntry createManualEntry(UUID actingUserId, UUID clinicId, JournalEntry entry);

    Optional<JournalEntry> recordCashExpenseRegistered(CashExpenseRegisteredEvent event);

    Optional<JournalEntry> recordCashExpenseVoided(com.jclinical.core.events.CashExpenseVoidedEvent event);

    Optional<JournalEntry> recordPurchaseOrderCreated(com.jclinical.core.events.PurchaseOrderCreatedEvent event);

    Optional<JournalEntry> recordPayrollPayment(PayrollPaymentRegisteredEvent event);
}
