package com.jclinical.automation.infra.config;

import com.jclinical.automation.domain.model.WhatsAppMessageReceivedEvent;
import com.jclinical.automation.domain.ports.in.ProcessInboundWhatsAppUseCase;
import com.jclinical.automation.domain.service.InboundWhatsAppProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** La conversacion avanza y sus respuestas quedan en cola en la misma transaccion. */
@Service
@Primary
@RequiredArgsConstructor
public class TransactionalInboundWhatsAppProcessor implements ProcessInboundWhatsAppUseCase {

    private final InboundWhatsAppProcessor processor;

    @Override
    @Transactional
    public void process(WhatsAppMessageReceivedEvent event) {
        processor.process(event);
    }
}
