package com.jclinical.integrations.domain.service;

import com.jclinical.integrations.domain.model.ExternalCalendarEvent;
import com.jclinical.integrations.domain.model.ExternalEventStatus;
import com.jclinical.integrations.domain.ports.in.ManageExternalCalendarEventsUseCase;
import com.jclinical.integrations.domain.ports.out.ExternalCalendarEventRepositoryPort;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class ManageExternalCalendarEventsService implements ManageExternalCalendarEventsUseCase {

    private final ExternalCalendarEventRepositoryPort repositoryPort;

    @Override
    public List<ExternalCalendarEvent> listPending(UUID clinicId, LocalDateTime from, LocalDateTime to) {
        return repositoryPort.findByClinicIdAndDateRangeAndStatus(clinicId, from, to, ExternalEventStatus.PENDING_REVIEW);
    }

    @Override
    public void dismiss(UUID clinicId, UUID eventId) {
        ExternalCalendarEvent event = repositoryPort.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Evento externo no encontrado"));
        if (!event.getClinicId().equals(clinicId)) {
            throw new IllegalArgumentException("El evento no pertenece a esta clínica");
        }
        event.setStatus(ExternalEventStatus.DISMISSED);
        event.setUpdatedAt(LocalDateTime.now());
        repositoryPort.save(event);
    }

    @Override
    public void markAsLinked(UUID clinicId, UUID eventId, UUID appointmentId) {
        ExternalCalendarEvent event = repositoryPort.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Evento externo no encontrado"));
        if (!event.getClinicId().equals(clinicId)) {
            throw new IllegalArgumentException("El evento no pertenece a esta clínica");
        }
        event.setStatus(ExternalEventStatus.LINKED);
        event.setLinkedAppointmentId(appointmentId);
        event.setUpdatedAt(LocalDateTime.now());
        repositoryPort.save(event);
    }
}
