package com.jclinical.accounting.infra.adapters.in.messaging;

import com.jclinical.accounting.domain.ports.in.ManageJournalUseCase;
import com.jclinical.accounting.infra.config.AccountingRabbitConfig;
import com.jclinical.core.events.PaymentRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentRegisteredListener {

    private final ManageJournalUseCase journalUseCase;

    @RabbitListener(queues = AccountingRabbitConfig.PAYMENT_REGISTERED_QUEUE, containerFactory = "domainEventListenerContainerFactory")
    public void onPaymentRegistered(PaymentRegisteredEvent event) {
        log.info(">>>> [ARE] Recibido PagoRegistrado para ticket {} (efectivo={}, otros={}, liquidado={})",
                event.ticketId(), event.cashAmount(), event.nonCashAmount(), event.fullyPaid());
        journalUseCase.recordPaymentRegistered(event)
                .ifPresentOrElse(
                        entry -> log.info(">>>> [ARE] Póliza generada: {} | Total: {}", entry.getId(), entry.totalCredits()),
                        () -> log.info(">>>> [ARE] Evento {} ya procesado o sin monto — no se generó póliza duplicada.", event.eventId())
                );
    }
}
