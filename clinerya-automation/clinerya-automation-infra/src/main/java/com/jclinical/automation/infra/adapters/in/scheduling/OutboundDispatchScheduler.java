package com.jclinical.automation.infra.adapters.in.scheduling;

import com.jclinical.automation.domain.ports.in.DispatchOutboundMessagesUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Vacia la cola de salida de WhatsApp: cada mensaje en su propia transaccion. */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboundDispatchScheduler {

    static final int MAX_PER_TICK = 50;

    private final DispatchOutboundMessagesUseCase dispatcher;

    @Scheduled(fixedDelayString = "${app.automation.whatsapp.dispatch-interval-ms:2000}",
            initialDelayString = "${app.automation.whatsapp.dispatch-initial-delay-ms:15000}")
    public void dispatch() {
        try {
            int sent = 0;
            while (sent < MAX_PER_TICK && dispatcher.dispatchNext()) {
                sent++;
            }
        } catch (RuntimeException exception) {
            log.error(">>>> [AUTOMATIZACION] Fallo el envio de la cola de WhatsApp", exception);
        }
    }
}
