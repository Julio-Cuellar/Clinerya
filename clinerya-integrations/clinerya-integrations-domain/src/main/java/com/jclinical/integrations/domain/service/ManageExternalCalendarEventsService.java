package com.jclinical.integrations.domain.service;

import com.jclinical.core.security.ClinicAccessDeniedException;
import com.jclinical.core.security.StaffPermission;
import com.jclinical.core.security.StaffPermissionCheckerPort;
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
    private final StaffPermissionCheckerPort permissionChecker;

    @Override
    public List<ExternalCalendarEvent> listPending(UUID clinicId, UUID actingUserId, LocalDateTime from, LocalDateTime to) {
        requireIntegrationsPermission(clinicId, actingUserId);
        return repositoryPort.findByClinicIdAndDateRangeAndStatus(clinicId, from, to, ExternalEventStatus.PENDING_REVIEW);
    }

    @Override
    public void dismiss(UUID clinicId, UUID actingUserId, UUID eventId) {
        requireIntegrationsPermission(clinicId, actingUserId);
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
    public void markAsLinked(UUID clinicId, UUID actingUserId, UUID eventId, UUID appointmentId) {
        requireIntegrationsPermission(clinicId, actingUserId);
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

    private void requireIntegrationsPermission(UUID clinicId, UUID actingUserId) {
        if (actingUserId == null) {
            throw new ClinicAccessDeniedException("Usuario no autenticado.");
        }
        if (!permissionChecker.hasPermission(clinicId, actingUserId, StaffPermission.MANAGE_INTEGRATIONS)) {
            throw new ClinicAccessDeniedException("No tienes permiso para gestionar las integraciones de esta clinica.");
        }
    }
}
