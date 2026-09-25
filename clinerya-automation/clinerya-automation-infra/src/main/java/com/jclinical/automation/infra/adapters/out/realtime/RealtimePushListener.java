package com.jclinical.automation.infra.adapters.out.realtime;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Entrega los avisos por STOMP despues del commit. Sin transaccion (fallbackExecution) se entregan de
 * inmediato. Un aviso perdido no rompe nada: la pantalla vuelve a consultar al reconectarse.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RealtimePushListener {

    private final SimpMessagingTemplate messaging;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void deliver(RealtimePush push) {
        try {
            messaging.convertAndSend(push.destination(), push.payload());
        } catch (RuntimeException exception) {
            log.warn(">>>> [AUTOMATIZACION] No se pudo entregar un aviso en tiempo real a {}", push.destination(), exception);
        }
    }
}
