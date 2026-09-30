package com.jclinical.automation.domain.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Mensaje en la cola de salida. {@code templateParameters} son los valores que llenan la plantilla
 * aprobada cuando la ventana de 24 h de WhatsApp esta cerrada. {@code templateName}: plantilla propia del
 * mensaje (el recordatorio de cita, con sus botones); null usa la general de la clinica.
 */
public record QueuedMessage(
        UUID id,
        UUID clinicId,
        String phone,
        Audience audience,
        OutboundReply reply,
        List<String> templateParameters,
        int attempts,
        LocalDateTime createdAt,
        String templateName
) {

    public enum Audience {
        PATIENT,
        DOCTOR
    }

    public QueuedMessage {
        templateParameters = templateParameters == null ? List.of() : List.copyOf(templateParameters);
    }

    public QueuedMessage(UUID id, UUID clinicId, String phone, Audience audience, OutboundReply reply,
                         List<String> templateParameters, int attempts, LocalDateTime createdAt) {
        this(id, clinicId, phone, audience, reply, templateParameters, attempts, createdAt, null);
    }
}
