package com.jclinical.integrations.infra.adapters.in.web;

import com.jclinical.integrations.domain.model.ExternalCalendarEvent;
import com.jclinical.integrations.domain.ports.in.ManageExternalCalendarEventsUseCase;
import com.jclinical.integrations.infra.service.ExternalEventLinkingOrchestrator;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clinics/{clinicId}/external-calendar-events")
@RequiredArgsConstructor
public class ExternalCalendarEventController {

    private final ManageExternalCalendarEventsUseCase manageExternalEventsUseCase;
    private final ExternalEventLinkingOrchestrator linkingOrchestrator;

    @GetMapping
    public ResponseEntity<List<ExternalCalendarEvent>> listPending(
            @PathVariable UUID clinicId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        List<ExternalCalendarEvent> events = manageExternalEventsUseCase.listPending(clinicId, from, to);
        return ResponseEntity.ok(events);
    }

    @PostMapping("/{eventId}/link")
    public ResponseEntity<Void> link(
            @PathVariable UUID clinicId,
            @PathVariable UUID eventId,
            @RequestBody LinkEventRequest request) {
        linkingOrchestrator.linkEventToPatient(clinicId, eventId, request.getPatientId(), request.getReason());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{eventId}/dismiss")
    public ResponseEntity<Void> dismiss(
            @PathVariable UUID clinicId,
            @PathVariable UUID eventId) {
        manageExternalEventsUseCase.dismiss(clinicId, eventId);
        return ResponseEntity.ok().build();
    }

    @Data
    public static class LinkEventRequest {
        private UUID patientId;
        private String reason;
    }
}
