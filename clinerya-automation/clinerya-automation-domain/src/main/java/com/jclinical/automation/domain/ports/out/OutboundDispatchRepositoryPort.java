package com.jclinical.automation.domain.ports.out;

import com.jclinical.automation.domain.model.QueuedMessage;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Lado de envio de la cola. {@link #lockNextDue} toma el mensaje pendiente mas antiguo que ya toca
 * enviar, sin saltarse a uno anterior del mismo celular (el orden de la conversacion se respeta).
 */
public interface OutboundDispatchRepositoryPort {

    Optional<QueuedMessage> lockNextDue(LocalDateTime now);

    void markSent(UUID id, String waMessageId, boolean viaTemplate, LocalDateTime at);

    void scheduleRetry(UUID id, int attempts, LocalDateTime nextAttemptAt, String error);

    void markFailed(UUID id, int attempts, String error, LocalDateTime at);
}
