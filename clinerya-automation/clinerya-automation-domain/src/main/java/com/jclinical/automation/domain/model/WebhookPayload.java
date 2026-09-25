package com.jclinical.automation.domain.model;

import java.util.List;

/** Lo que trae una llamada del webhook: mensajes nuevos y avisos de estado de lo enviado. */
public record WebhookPayload(List<WhatsAppInboundMessage> messages, List<DeliveryStatusUpdate> statuses) {

    public static final WebhookPayload EMPTY = new WebhookPayload(List.of(), List.of());

    public WebhookPayload {
        messages = messages == null ? List.of() : List.copyOf(messages);
        statuses = statuses == null ? List.of() : List.copyOf(statuses);
    }
}
