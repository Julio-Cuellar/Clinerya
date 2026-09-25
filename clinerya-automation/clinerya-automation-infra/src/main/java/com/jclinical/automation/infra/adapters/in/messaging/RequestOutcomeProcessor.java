package com.jclinical.automation.infra.adapters.in.messaging;

import com.jclinical.automation.domain.model.AppointmentRequestResolvedEvent;
import com.jclinical.automation.domain.ports.in.HandleRequestOutcomeUseCase;
import com.jclinical.automation.domain.ports.out.OutboundMessageQueuePort;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mueve la conversacion y encola el mensaje al paciente en una sola transaccion. Si el evento llega
 * repetido, la conversacion ya no lo espera y no se encola nada.
 */
public class RequestOutcomeProcessor {

    private final HandleRequestOutcomeUseCase conversations;
    private final OutboundMessageQueuePort outbound;

    public RequestOutcomeProcessor(HandleRequestOutcomeUseCase conversations, OutboundMessageQueuePort outbound) {
        this.conversations = conversations;
        this.outbound = outbound;
    }

    @Transactional
    public void process(AppointmentRequestResolvedEvent event) {
        conversations.onRequestResolved(event).ifPresent(outbound::enqueue);
    }
}
