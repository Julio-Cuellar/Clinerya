package com.jclinical.integrations.infra.service;

import com.jclinical.agenda.domain.model.Appointment;
import com.jclinical.agenda.domain.ports.in.ManageAppointmentsUseCase;
import com.jclinical.integrations.domain.model.CalendarCredentials;
import com.jclinical.integrations.domain.model.CalendarEventDraft;
import com.jclinical.integrations.domain.ports.out.CalendarCredentialsRepositoryPort;
import com.jclinical.integrations.domain.ports.out.CalendarEventPort;
import com.jclinical.integrations.domain.ports.out.GoogleOAuthPort;
import com.jclinical.patients.domain.model.Patient;
import com.jclinical.patients.domain.ports.in.GetPatientUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Orquesta la sincronización unidireccional (app -> Google) de citas hacia el calendario
 * personal del doctor conectado. Vive en infra porque cruza el límite del módulo agenda
 * (invoca su in-port ManageAppointmentsUseCase directamente en vez de vía eventos),
 * manteniendo el dominio de integrations libre de esa dependencia cruzada.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CalendarSyncOrchestrator {

    private static final String DEFAULT_SUMMARY = "Cita médica";

    private final CalendarCredentialsRepositoryPort credentialsRepository;
    private final CredentialsTokenRefresher tokenRefresher;
    private final CalendarEventPort calendarEventPort;
    private final ManageAppointmentsUseCase appointmentsUseCase;
    private final GetPatientUseCase getPatientUseCase;

    public void onAppointmentScheduled(UUID clinicId, UUID appointmentId, UUID doctorStaffId, LocalDateTime start, LocalDateTime end) {
        findFreshCredentials(clinicId, doctorStaffId).ifPresent(credentials -> {
            Appointment appointment = appointmentsUseCase.getAppointment(appointmentId, clinicId);
            CalendarEventDraft draft = buildDraft(appointment, start, end);
            String externalEventId = calendarEventPort.createEvent(credentials, draft);
            if (externalEventId != null) {
                appointmentsUseCase.attachExternalCalendarEvent(appointmentId, clinicId, externalEventId);
            }
        });
    }

    public void onAppointmentRescheduled(UUID clinicId, UUID appointmentId, LocalDateTime newStart, LocalDateTime newEnd) {
        Appointment appointment = appointmentsUseCase.getAppointment(appointmentId, clinicId);
        if (appointment.getExternalCalendarEventId() == null) {
            return;
        }
        findFreshCredentials(clinicId, appointment.getDoctorStaffId()).ifPresent(credentials -> {
            CalendarEventDraft draft = buildDraft(appointment, newStart, newEnd);
            calendarEventPort.updateEvent(credentials, appointment.getExternalCalendarEventId(), draft);
        });
    }

    private CalendarEventDraft buildDraft(Appointment appointment, LocalDateTime start, LocalDateTime end) {
        String patientName = getPatientUseCase.getPatientById(appointment.getPatientId())
                .map(this::fullName)
                .orElse(null);
        String summary = patientName != null ? DEFAULT_SUMMARY + " - " + patientName : DEFAULT_SUMMARY;
        return new CalendarEventDraft(summary, appointment.getReason(), start, end, appointment.getId());
    }

    private String fullName(Patient patient) {
        return (patient.getFirstName() + " " + patient.getLastNamePaterno()).trim();
    }

    public void onAppointmentCancelled(UUID clinicId, UUID appointmentId) {
        Appointment appointment = appointmentsUseCase.getAppointment(appointmentId, clinicId);
        if (appointment.getExternalCalendarEventId() == null) {
            return;
        }
        findFreshCredentials(clinicId, appointment.getDoctorStaffId()).ifPresent(credentials ->
                calendarEventPort.deleteEvent(credentials, appointment.getExternalCalendarEventId()));
    }

    public void onAppointmentDeleted(UUID clinicId, UUID doctorStaffId, String externalCalendarEventId) {
        if (externalCalendarEventId == null) {
            return;
        }
        // No se puede volver a leer la cita (ya se borró), así que el evento de dominio
        // trae doctorStaffId/externalCalendarEventId directamente para evitar el fetch.
        findFreshCredentials(clinicId, doctorStaffId).ifPresent(credentials ->
                calendarEventPort.deleteEvent(credentials, externalCalendarEventId));
    }

    private Optional<CalendarCredentials> findFreshCredentials(UUID clinicId, UUID doctorStaffId) {
        return credentialsRepository.findByClinicIdAndStaffId(clinicId, doctorStaffId)
                .map(tokenRefresher::ensureFreshToken);
    }
}

