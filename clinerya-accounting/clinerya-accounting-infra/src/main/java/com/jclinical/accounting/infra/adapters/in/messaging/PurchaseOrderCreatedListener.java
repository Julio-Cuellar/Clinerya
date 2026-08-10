package com.jclinical.accounting.infra.adapters.in.messaging;

import com.jclinical.accounting.domain.ports.in.ManageJournalUseCase;
import com.jclinical.accounting.infra.config.AccountingRabbitConfig;
import com.jclinical.core.events.PurchaseOrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PurchaseOrderCreatedListener {

    private final ManageJournalUseCase journalUseCase;

    @RabbitListener(queues = AccountingRabbitConfig.PURCHASE_ORDER_CREATED_QUEUE, containerFactory = "domainEventListenerContainerFactory")
    public void onPurchaseOrderCreated(PurchaseOrderCreatedEvent event) {
        log.info(">>>> [ARE] Recibido PurchaseOrderCreated para pedido {} (Folio: {})", event.purchaseOrderId(), event.folio());
        journalUseCase.recordPurchaseOrderCreated(event)
                .ifPresentOrElse(
                        entry -> log.info(">>>> [ARE] Póliza generada: {} | Total: {}", entry.getId(), entry.totalDebits()),
                        () -> log.info(">>>> [ARE] Evento {} ya procesado, sin banco o sin monto — no se generó póliza.", event.eventId())
                );
    }
}
