package com.jclinical.integrations.domain.ports.out;

import com.jclinical.integrations.domain.model.CalendarCredentials;
import com.jclinical.integrations.domain.model.CalendarEventDraft;

import java.time.LocalDateTime;
import java.util.List;

public interface CalendarEventPort {

    String createEvent(CalendarCredentials credentials, CalendarEventDraft draft);

    void updateEvent(CalendarCredentials credentials, String externalEventId, CalendarEventDraft draft);

    void deleteEvent(CalendarCredentials credentials, String externalEventId);

    EventsPage listEvents(CalendarCredentials credentials, String syncToken);

    record EventsPage(List<ExternalEventSnapshot> events, String nextSyncToken, boolean tokenInvalid) {}

    record ExternalEventSnapshot(
            String googleEventId,
            String summary,
            String description,
            LocalDateTime start,
            LocalDateTime end,
            boolean cancelled,
            boolean createdByJClinical
    ) {}
}
