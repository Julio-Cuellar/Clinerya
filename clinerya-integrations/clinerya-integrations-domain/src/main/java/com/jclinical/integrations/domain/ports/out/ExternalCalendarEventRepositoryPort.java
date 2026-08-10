package com.jclinical.integrations.domain.ports.out;

import com.jclinical.integrations.domain.model.ExternalCalendarEvent;
import com.jclinical.integrations.domain.model.ExternalEventStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExternalCalendarEventRepositoryPort {
    ExternalCalendarEvent save(ExternalCalendarEvent event);
    Optional<ExternalCalendarEvent> findById(UUID id);
    Optional<ExternalCalendarEvent> findByClinicIdAndGoogleEventId(UUID clinicId, String googleEventId);
    List<ExternalCalendarEvent> findByClinicIdAndDateRangeAndStatus(

            UUID clinicId,
            LocalDateTime from,
            LocalDateTime to,
            ExternalEventStatus status
    );
    void delete(ExternalCalendarEvent event);
}
