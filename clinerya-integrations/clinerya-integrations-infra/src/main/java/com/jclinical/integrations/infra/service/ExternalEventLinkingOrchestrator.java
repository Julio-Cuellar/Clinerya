package com.jclinical.integrations.infra.service;

import com.jclinical.agenda.domain.model.Appointment;
import com.jclinical.agenda.domain.ports.in.ManageAppointmentsUseCase;
import com.jclinical.integrations.domain.model.ExternalCalendarEvent;
import com.jclinical.integrations.domain.model.ExternalEventStatus;
import com.jclinical.integrations.domain.ports.in.ManageExternalCalendarEventsUseCase;
import com.jclinical.integrations.domain.ports.out.ExternalCalendarEventRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalEventLinkingOrchestrator {

    private final ExternalCalendarEventRepositoryPort externalEventRepository;
    private final ManageExternalCalendarEventsUseCase manageExternalEventsUseCase;
    private final ManageAppointmentsUseCase appointmentsUseCase;

    @Transactional
    public void linkEventToPatient(UUID clinicId, UUID eventId, UUID patientId, String reason) {
        ExternalCalendarEvent event = externalEventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Evento externo no encontrado"));

        if (!event.getClinicId().equals(clinicId)) {
            throw new IllegalArgumentException("El evento no pertenece a esta clínica");
        }

        if (event.getStatus() != ExternalEventStatus.PENDING_REVIEW) {
            throw new IllegalStateException("El evento ya ha sido procesado (estado actual: " + event.getStatus() + ")");
        }

        String finalReason = (reason == null || reason.trim().isEmpty()) ? event.getSummary() : reason.trim();

        // 1. Crear la cita en el módulo agenda
        ManageAppointmentsUseCase.CreateAppointmentCommand command = new ManageAppointmentsUseCase.CreateAppointmentCommand(
                patientId,
                event.getStaffId(),
                null, // quotationId
                null, // quotationItemId
                event.getStartTime(),
                event.getEndTime(),
                finalReason,
                "Creado a partir de evento externo de Google Calendar: " + event.getGoogleEventId()
        );

        Appointment appointment = appointmentsUseCase.createAppointment(clinicId, command);

        // 2. Marcar el evento externo como LINKED
        manageExternalEventsUseCase.markAsLinked(clinicId, eventId, appointment.getId());

        // 3. Enlazar la nueva Appointment al googleEventId
        appointmentsUseCase.attachExternalCalendarEvent(appointment.getId(), clinicId, event.getGoogleEventId());

        log.info("Evento externo {} vinculado exitosamente a cita {} y paciente {}",
                eventId, appointment.getId(), patientId);
    }
}
