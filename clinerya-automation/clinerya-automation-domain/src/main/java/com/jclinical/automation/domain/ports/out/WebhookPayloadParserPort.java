package com.jclinical.automation.domain.ports.out;

import com.jclinical.automation.domain.model.WhatsAppInboundMessage;

import java.util.List;

/** Lee el JSON de Meta. Un cuerpo ilegible o sin mensajes (solo estados) devuelve una lista vacia. */
public interface WebhookPayloadParserPort {

    List<WhatsAppInboundMessage> parse(byte[] rawBody);
}
