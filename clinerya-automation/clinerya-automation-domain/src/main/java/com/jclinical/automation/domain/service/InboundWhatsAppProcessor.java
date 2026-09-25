package com.jclinical.automation.domain.service;

import com.jclinical.automation.domain.model.WhatsAppMessageReceivedEvent;
import com.jclinical.automation.domain.ports.in.HandleInboundMessageUseCase;
import com.jclinical.automation.domain.ports.in.ProcessInboundWhatsAppUseCase;
import com.jclinical.automation.domain.ports.out.OutboundMessageQueuePort;

public class InboundWhatsAppProcessor implements ProcessInboundWhatsAppUseCase {

    public InboundWhatsAppProcessor(HandleInboundMessageUseCase conversations, OutboundMessageQueuePort outbound) {
    }

    @Override
    public void process(WhatsAppMessageReceivedEvent event) {
        throw new UnsupportedOperationException("pendiente");
    }
}
