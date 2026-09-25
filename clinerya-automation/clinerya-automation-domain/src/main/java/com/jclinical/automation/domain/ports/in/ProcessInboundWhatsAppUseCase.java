package com.jclinical.automation.domain.ports.in;

import com.jclinical.automation.domain.model.WhatsAppMessageReceivedEvent;

/** Procesa en segundo plano un mensaje aceptado: conversacion y respuestas en cola. */
public interface ProcessInboundWhatsAppUseCase {

    void process(WhatsAppMessageReceivedEvent event);
}
