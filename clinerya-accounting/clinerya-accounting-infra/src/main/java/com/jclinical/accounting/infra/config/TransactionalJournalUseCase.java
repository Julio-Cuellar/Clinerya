package com.jclinical.accounting.infra.config;

import com.jclinical.accounting.domain.model.JournalEntry;
import com.jclinical.accounting.domain.model.JournalQueryResult;
import com.jclinical.accounting.domain.ports.in.ManageJournalUseCase;
import com.jclinical.accounting.domain.service.JournalEntryService;
import com.jclinical.core.events.PurchaseOrderCreatedEvent;
import com.jclinical.core.events.CashExpenseRegisteredEvent;
import com.jclinical.core.events.CashExpenseVoidedEvent;
import com.jclinical.core.events.ConsumoConciliadoEvent;
import com.jclinical.core.events.MermaCaducidadEvent;
import com.jclinical.core.events.PaymentRegisteredEvent;
import com.jclinical.core.events.PayrollPaymentRegisteredEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.LocalDate;

@Service
@Primary
@RequiredArgsConstructor
public class TransactionalJournalUseCase implements ManageJournalUseCase {

    private final JournalEntryService journalEntryService;

    @Override
    @Transactional
    public Optional<JournalEntry> recordConsumptionReconciled(ConsumoConciliadoEvent event) {
        return journalEntryService.recordConsumptionReconciled(event);
    }

    @Override
    @Transactional
    public Optional<JournalEntry> recordPaymentRegistered(PaymentRegisteredEvent event) {
        return journalEntryService.recordPaymentRegistered(event);
    }

    @Override
    @Transactional
    public Optional<JournalEntry> recordSupplyWasted(MermaCaducidadEvent event) {
        return journalEntryService.recordSupplyWasted(event);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JournalEntry> listByClinic(UUID clinicId) {
        return journalEntryService.listByClinic(clinicId);
    }

    @Override
    @Transactional(readOnly = true)
    public JournalQueryResult queryByClinic(UUID clinicId, LocalDate from, LocalDate to, String search, String sourceEventType, int page, int size) {
        return journalEntryService.queryByClinic(clinicId, from, to, search, sourceEventType, page, size);
    }

    @Override
    @Transactional
    public JournalEntry createManualEntry(UUID clinicId, JournalEntry entry) {
        return journalEntryService.createManualEntry(clinicId, entry);
    }

    @Override
    @Transactional
    public Optional<JournalEntry> recordCashExpenseRegistered(CashExpenseRegisteredEvent event) {
        return journalEntryService.recordCashExpenseRegistered(event);
    }

    @Override
    @Transactional
    public Optional<JournalEntry> recordCashExpenseVoided(CashExpenseVoidedEvent event) {
        return journalEntryService.recordCashExpenseVoided(event);
    }

    @Override
    @Transactional
    public Optional<JournalEntry> recordPurchaseOrderCreated(PurchaseOrderCreatedEvent event) {
        return journalEntryService.recordPurchaseOrderCreated(event);
    }

    @Override
    @Transactional
    public Optional<JournalEntry> recordPayrollPayment(PayrollPaymentRegisteredEvent event) {
        return journalEntryService.recordPayrollPayment(event);
    }
}
