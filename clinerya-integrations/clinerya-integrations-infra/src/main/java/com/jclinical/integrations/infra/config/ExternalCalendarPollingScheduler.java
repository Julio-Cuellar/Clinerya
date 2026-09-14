package com.jclinical.integrations.infra.config;

import com.jclinical.integrations.domain.model.CalendarCredentials;
import com.jclinical.integrations.domain.model.ExternalCalendarEvent;
import com.jclinical.integrations.domain.model.ExternalEventStatus;
import com.jclinical.integrations.domain.ports.out.CalendarCredentialsRepositoryPort;
import com.jclinical.integrations.domain.ports.out.CalendarEventPort;
import com.jclinical.integrations.domain.ports.out.ExternalCalendarEventRepositoryPort;
import com.jclinical.integrations.infra.service.CredentialsTokenRefresher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.jclinical.agenda.domain.ports.in.ManageAppointmentsUseCase;
import com.jclinical.agenda.domain.ports.out.AppointmentRepositoryPort;
import com.jclinical.agenda.domain.model.Appointment;
import com.jclinical.agenda.domain.model.AppointmentStatus;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExternalCalendarPollingScheduler {

    private final CalendarCredentialsRepositoryPort credentialsRepository;
    private final CredentialsTokenRefresher tokenRefresher;
    private final CalendarEventPort calendarEventPort;
    private final ExternalCalendarEventRepositoryPort externalEventRepository;
    private final ManageAppointmentsUseCase appointmentsUseCase;
    private final AppointmentRepositoryPort appointmentRepository;


    @Scheduled(fixedDelayString = "PT30S", initialDelayString = "PT15S")
    public void pollExternalEvents() {
        List<CalendarCredentials> allCredentials = credentialsRepository.findAll();
        for (CalendarCredentials credentials : allCredentials) {
            try {
                processCredentials(credentials);
            } catch (WebClientRequestException e) {
                log.warn("Google Calendar no estuvo disponible para el staff {} en clínica {}. "
                                + "El polling se reintentará en el siguiente ciclo: {}",
                        credentials.getStaffId(), credentials.getClinicId(), e.getMessage());
            } catch (Exception e) {
                log.error("Error procesando polling de calendario para el staff: {} en clínica: {}",
                        credentials.getStaffId(), credentials.getClinicId(), e);
            }
        }
    }

    private void processCredentials(CalendarCredentials credentials) {
        // 1. Refrescar token
        CalendarCredentials fresh = tokenRefresher.ensureFreshToken(credentials);

        // 2. Listar eventos usando syncToken
        CalendarEventPort.EventsPage page = calendarEventPort.listEvents(fresh, fresh.getCalendarSyncToken());

        // 3. Manejo de token inválido
        if (page.tokenInvalid()) {
            log.info("Token de sincronización inválido para staff {}. Limpiando syncToken para resync completo.", fresh.getStaffId());
            fresh.setCalendarSyncToken(null);
            credentialsRepository.save(fresh);
            return;
        }

        // 4. Procesar eventos obtenidos (cada evento aislado: uno inválido no debe bloquear
        // el resto del lote ni impedir que avance el syncToken).
        for (CalendarEventPort.ExternalEventSnapshot snap : page.events()) {
            try {
                processSnapshot(fresh, snap);
            } catch (Exception e) {
                log.warn("No se pudo procesar el evento de Google Calendar {} para staff {}, se omite.",
                        snap.googleEventId(), fresh.getStaffId(), e);
            }
        }

        // 5. Guardar nuevo syncToken
        if (page.nextSyncToken() != null && !page.nextSyncToken().equals(fresh.getCalendarSyncToken())) {
            fresh.setCalendarSyncToken(page.nextSyncToken());
            credentialsRepository.save(fresh);
        }
    }

    private void processSnapshot(CalendarCredentials fresh, CalendarEventPort.ExternalEventSnapshot snap) {
        // Si importPastEvents es false, ignoramos eventos no cancelados que comiencen antes de hoy.
        if (!fresh.isImportPastEvents() && !snap.cancelled() && snap.start().isBefore(LocalDateTime.now().toLocalDate().atStartOfDay())) {
            return;
        }

        Optional<Appointment> existingOpt = appointmentRepository
                .findByClinicIdAndExternalCalendarEventId(fresh.getClinicId(), snap.googleEventId());

        if (snap.cancelled()) {
            // Eventos cancelados: si existen activos en JClinical, los cancelamos
            existingOpt.ifPresent(existing -> {
                if (existing.getStatus() != AppointmentStatus.CANCELLED && existing.getStatus() != AppointmentStatus.NO_SHOW) {
                    appointmentsUseCase.transitionStatus(existing.getId(), fresh.getClinicId(), AppointmentStatus.CANCELLED, null, null);
                    log.info("Cita en JClinical cancelada por cancelación de evento en Google: {}", existing.getId());
                }
            });
            return;
        }

        if (snap.createdByJClinical()) {
            return;
        }

        if (existingOpt.isPresent()) {
            Appointment existing = existingOpt.get();
            // Si ya existe y cambió el horario en Google, reagendamos localmente
            if (!existing.getScheduledStart().equals(snap.start()) || !existing.getScheduledEnd().equals(snap.end())) {
                appointmentsUseCase.rescheduleAppointment(existing.getId(), fresh.getClinicId(), snap.start(), snap.end(), true);
                log.info("Cita en JClinical reagendada por cambio de horario en Google: {}", existing.getId());
            }
            return;
        }

        // Si no existe, creamos directamente una cita libre en JClinical
        ManageAppointmentsUseCase.CreateAppointmentCommand command = new ManageAppointmentsUseCase.CreateAppointmentCommand(
                null, // patientId = null (cita libre / sin asignar todavía)
                fresh.getStaffId(),
                null, // roomId = null
                null,
                null,
                java.util.List.of(),
                snap.start(),
                snap.end(),
                snap.summary() != null && !snap.summary().trim().isEmpty() ? snap.summary() : "Consulta Externa (Google)",
                snap.description() != null ? snap.description() : "Importado automáticamente de Google Calendar",
                true
        );
        Appointment created = appointmentsUseCase.createAppointment(fresh.getClinicId(), command);
        appointmentsUseCase.attachExternalCalendarEvent(created.getId(), fresh.getClinicId(), snap.googleEventId());
        log.info("Cita libre autocreada desde evento de Google Calendar: {}", created.getId());
    }
}
