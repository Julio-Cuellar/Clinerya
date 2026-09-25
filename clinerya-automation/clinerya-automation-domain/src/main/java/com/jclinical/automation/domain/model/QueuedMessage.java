package com.jclinical.automation.domain.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Mensaje en la cola de salida. {@code templateParameters} son los valores que llenan la plantilla
 * aprobada cuando la ventana de 24 h de WhatsApp esta cerrada.
 */
public record QueuedMessage(
        UUID id,
        UUID clinicId,
        String phone,
        Audience audience,
        OutboundReply reply,
        List<String> templateParameters,
        int attempts,
        LocalDateTime createdAt
) {

    public enum Audience {
        PATIENT,
        DOCTOR
    }

    public QueuedMessage {
        templateParameters = templateParameters == null ? List.of() : List.copyOf(templateParameters);
    }
}
