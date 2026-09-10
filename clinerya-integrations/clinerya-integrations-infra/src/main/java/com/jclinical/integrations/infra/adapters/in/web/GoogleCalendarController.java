package com.jclinical.integrations.infra.adapters.in.web;

import com.jclinical.integrations.domain.ports.in.ManageCalendarIntegrationUseCase;
import com.jclinical.users.infra.security.CurrentUserResolver;
import com.jclinical.integrations.infra.adapters.in.web.dto.AuthorizationUrlResponse;
import com.jclinical.integrations.infra.adapters.in.web.dto.CalendarConnectionStatusResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.jclinical.integrations.infra.adapters.in.web.dto.CalendarPreferencesRequest;

import java.util.UUID;

/**
 * Endpoints autenticados, con contexto de clínica/staff conocido (a diferencia del
 * callback OAuth, que usa una URL fija — ver GoogleCalendarCallbackController).
 */
@RestController
@RequestMapping("/api/v1/clinics/{clinicId}/staff/{staffId}/google-calendar")
@RequiredArgsConstructor
@Slf4j
public class GoogleCalendarController {

    private final ManageCalendarIntegrationUseCase calendarIntegrationUseCase;
    private final CurrentUserResolver currentUserResolver;

    @GetMapping("/connect")
    public ResponseEntity<AuthorizationUrlResponse> connect(
            @PathVariable UUID clinicId,
            @PathVariable UUID staffId,
            @RequestParam(defaultValue = "false") boolean importPastEvents) {
        String authorizationUrl = calendarIntegrationUseCase.getAuthorizationUrl(
                clinicId, currentUserResolver.getCurrentUserId(), staffId, importPastEvents);
        return ResponseEntity.ok(new AuthorizationUrlResponse(authorizationUrl));
    }


    @DeleteMapping
    public ResponseEntity<Void> disconnect(
            @PathVariable UUID clinicId,
            @PathVariable UUID staffId) {
        calendarIntegrationUseCase.disconnect(clinicId, currentUserResolver.getCurrentUserId(), staffId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/status")
    public ResponseEntity<CalendarConnectionStatusResponse> status(
            @PathVariable UUID clinicId,
            @PathVariable UUID staffId) {
        var status = calendarIntegrationUseCase.getStatus(clinicId, currentUserResolver.getCurrentUserId(), staffId);
        return ResponseEntity.ok(new CalendarConnectionStatusResponse(status.connected(), status.email(), status.importPastEvents()));
    }

    @PatchMapping("/preferences")
    public ResponseEntity<Void> updatePreferences(
            @PathVariable UUID clinicId,
            @PathVariable UUID staffId,
            @RequestBody CalendarPreferencesRequest request) {
        calendarIntegrationUseCase.updatePreferences(
                clinicId, currentUserResolver.getCurrentUserId(), staffId, request.importPastEvents());
        return ResponseEntity.ok().build();
    }
}

