package com.jclinical.integrations.domain.ports.in;

import com.jclinical.integrations.domain.model.ExternalCalendarEvent;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ManageExternalCalendarEventsUseCase {
    List<ExternalCalendarEvent> listPending(UUID clinicId, UUID actingUserId, LocalDateTime from, LocalDateTime to);
    void dismiss(UUID clinicId, UUID actingUserId, UUID eventId);
    void markAsLinked(UUID clinicId, UUID actingUserId, UUID eventId, UUID appointmentId);
}
