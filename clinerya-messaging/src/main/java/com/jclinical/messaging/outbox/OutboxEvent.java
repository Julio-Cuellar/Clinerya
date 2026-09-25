package com.jclinical.messaging.outbox;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Evento de dominio en espera de entregarse al broker. {@code payloadType} es el nombre de la
 * clase del record original, para reconstruirlo tal cual antes de enviarlo.
 */
public record OutboxEvent(
        UUID id,
        String routingKey,
        String payloadType,
        String payload,
        LocalDateTime createdAt
) {}
