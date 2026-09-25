package com.jclinical.automation.domain.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * El medico respondio una solicitud (o vencio su plazo). La conversacion del paciente reacciona a
 * este evento; viaja por el outbox, asi que puede llegar mas de una vez.
 */
public record AppointmentRequestResolvedEvent(
        UUID eventId,
        UUID clinicId,
        UUID requestId,
        UUID conversationId,
        Outcome outcome,
        String doctorName,
        LocalDateTime start,
        LocalDateTime end,
        String reason,
        List<AvailableSlot> options,
        LocalDateTime occurredAt
) {

    public enum Outcome {
        BOOKED,
        REJECTED,
        OPTIONS_PROPOSED,
        EXPIRED
    }

    public AppointmentRequestResolvedEvent {
        options = options == null ? List.of() : List.copyOf(options);
    }
}
