package com.jclinical.accounting.infra.adapters.in.messaging;

import com.jclinical.accounting.domain.ports.in.ManageJournalUseCase;
import com.jclinical.accounting.infra.config.AccountingRabbitConfig;
import com.jclinical.core.events.MermaCaducidadEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SupplyWastedListener {

    private final ManageJournalUseCase journalUseCase;

    @RabbitListener(queues = AccountingRabbitConfig.SUPPLY_WASTED_QUEUE, containerFactory = "domainEventListenerContainerFactory")
    public void onSupplyWasted(MermaCaducidadEvent event) {
        log.info(">>>> [ARE] Recibido MermaCaducidad para material {} (cantidad={}, costoUnitario={})",
                event.materialId(), event.quantity(), event.unitCostAtMovement());
        journalUseCase.recordSupplyWasted(event)
                .ifPresentOrElse(
                        entry -> log.info(">>>> [ARE] Póliza generada: {} | Total: {}", entry.getId(), entry.totalCredits()),
                        () -> log.info(">>>> [ARE] Evento {} ya procesado o sin monto — no se generó póliza duplicada.", event.eventId())
                );
    }
}
