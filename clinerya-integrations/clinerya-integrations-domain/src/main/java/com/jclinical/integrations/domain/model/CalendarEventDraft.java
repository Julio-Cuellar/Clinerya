package com.jclinical.integrations.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * appointmentId se etiqueta en el evento de Google (extendedProperties) para que el
 * polling de eventos externos pueda distinguir "esto lo creó la propia app" y no lo
 * vuelva a traer como pendiente de revisión.
 */
public record CalendarEventDraft(
        String summary,
        String description,
        LocalDateTime start,
        LocalDateTime end,
        UUID appointmentId
) {}
