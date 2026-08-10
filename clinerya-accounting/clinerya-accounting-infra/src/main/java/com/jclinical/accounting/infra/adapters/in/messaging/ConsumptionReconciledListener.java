package com.jclinical.accounting.infra.adapters.in.messaging;

import com.jclinical.accounting.domain.ports.in.ManageJournalUseCase;
import com.jclinical.accounting.infra.config.AccountingRabbitConfig;
import com.jclinical.core.events.ConsumoConciliadoEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ConsumptionReconciledListener {

    private final ManageJournalUseCase journalUseCase;

    @RabbitListener(queues = AccountingRabbitConfig.CONSUMPTION_RECONCILED_QUEUE, containerFactory = "domainEventListenerContainerFactory")
    public void onConsumptionReconciled(ConsumoConciliadoEvent event) {
        log.info(">>>> [ARE] Recibido ConsumoConciliado para visita {} ({} materiales)", event.visitId(), event.materials().size());
        journalUseCase.recordConsumptionReconciled(event)
                .ifPresentOrElse(
                        entry -> log.info(">>>> [ARE] Póliza generada: {} | Total: {}", entry.getId(), entry.totalDebits()),
                        () -> log.info(">>>> [ARE] Evento {} ya procesado o sin monto — no se generó póliza duplicada.", event.eventId())
                );
    }
}
