package com.jclinical.automation.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Mensaje aceptado por el webhook, listo para procesarse en segundo plano. Viaja por el outbox:
 * Meta recibe su 200 de inmediato y la conversacion (con Gemini) corre despues.
 */
public record WhatsAppMessageReceivedEvent(
        UUID eventId,
        UUID clinicId,
        String waMessageId,
        String fromPhone,
        WhatsAppInboundMessage.Kind kind,
        String text,
        String selectedOptionId,
        LocalDateTime receivedAt
) {}
