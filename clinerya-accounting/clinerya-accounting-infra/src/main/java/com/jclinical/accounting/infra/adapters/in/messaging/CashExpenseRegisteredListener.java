package com.jclinical.accounting.infra.adapters.in.messaging;

import com.jclinical.accounting.domain.ports.in.ManageJournalUseCase;
import com.jclinical.accounting.infra.config.AccountingRabbitConfig;
import com.jclinical.core.events.CashExpenseRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CashExpenseRegisteredListener {

    private final ManageJournalUseCase journalUseCase;

    @RabbitListener(queues = AccountingRabbitConfig.CASH_EXPENSE_REGISTERED_QUEUE, containerFactory = "domainEventListenerContainerFactory")
    public void onCashExpenseRegistered(CashExpenseRegisteredEvent event) {
        log.info(">>>> [ARE] Recibido EgresoCajaRegistrado para egreso {} (monto={})",
                event.cashExpenseId(), event.amount());
        journalUseCase.recordCashExpenseRegistered(event)
                .ifPresentOrElse(
                        entry -> log.info(">>>> [ARE] Póliza generada por egreso de caja: {} | Total: {}", entry.getId(), entry.totalCredits()),
                        () -> log.info(">>>> [ARE] Evento {} ya procesado o sin monto — no se generó póliza duplicada.", event.eventId())
                );
    }
}
