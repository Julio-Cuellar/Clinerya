package com.jclinical.automation.infra.adapters.in.messaging;

import com.jclinical.automation.domain.model.AppointmentRequestResolvedEvent;
import com.jclinical.automation.domain.ports.in.HandleRequestOutcomeUseCase;
import com.jclinical.automation.domain.ports.out.OutboundMessageQueuePort;

public class RequestOutcomeProcessor {

    public RequestOutcomeProcessor(HandleRequestOutcomeUseCase conversations, OutboundMessageQueuePort outbound) {
    }

    public void process(AppointmentRequestResolvedEvent event) {
        throw new UnsupportedOperationException("pendiente");
    }
}
