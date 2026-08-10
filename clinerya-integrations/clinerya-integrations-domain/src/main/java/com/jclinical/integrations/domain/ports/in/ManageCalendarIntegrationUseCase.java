package com.jclinical.integrations.domain.ports.in;

import java.util.UUID;

public interface ManageCalendarIntegrationUseCase {

    String getAuthorizationUrl(UUID clinicId, UUID staffId, boolean importPastEvents);


    void handleOAuthCallback(String state, String code);

    void disconnect(UUID clinicId, UUID staffId);

    CalendarConnectionStatus getStatus(UUID clinicId, UUID staffId);

    void updatePreferences(UUID clinicId, UUID staffId, boolean importPastEvents);

    record CalendarConnectionStatus(boolean connected, String email, boolean importPastEvents) {}
}

