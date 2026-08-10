package com.jclinical.accounting.infra.adapters.in.messaging;

import com.jclinical.accounting.domain.ports.in.ManageJournalUseCase;
import com.jclinical.accounting.infra.config.AccountingRabbitConfig;
import com.jclinical.core.events.CashExpenseVoidedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CashExpenseVoidedListener {

    private final ManageJournalUseCase journalUseCase;

    @RabbitListener(queues = AccountingRabbitConfig.CASH_EXPENSE_VOIDED_QUEUE, containerFactory = "domainEventListenerContainerFactory")
    public void onCashExpenseVoided(CashExpenseVoidedEvent event) {
        log.info(">>>> [ARE] Recibido EgresoCajaAnulado para egreso {}", event.cashExpenseId());
        journalUseCase.recordCashExpenseVoided(event)
                .ifPresentOrElse(
                        entry -> log.info(">>>> [ARE] Póliza de reversa generada: {} | Total: {}", entry.getId(), entry.totalCredits()),
                        () -> log.info(">>>> [ARE] Evento {} ya procesado — no se generó póliza duplicada.", event.eventId())
                );
    }
}
