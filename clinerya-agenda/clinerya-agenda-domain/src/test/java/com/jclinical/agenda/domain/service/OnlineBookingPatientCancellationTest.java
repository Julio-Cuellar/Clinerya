package com.jclinical.agenda.domain.service;

import com.jclinical.agenda.domain.model.Appointment;
import com.jclinical.agenda.domain.model.AppointmentStatus;
import com.jclinical.agenda.domain.ports.in.ManageAppointmentsUseCase;
import com.jclinical.agenda.domain.ports.in.OnlineBookingUseCase.AppointmentNotCancellableException;
import com.jclinical.agenda.domain.ports.out.AppointmentRepositoryPort;
import com.jclinical.agenda.domain.ports.out.OnlineBookingSettingsPort;
import com.jclinical.agenda.domain.ports.out.SlotHoldRepositoryPort;
import com.jclinical.agenda.domain.ports.out.StaffValidatorPort;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * El paciente cancela su propia cita por WhatsApp. Solo una cita suya, vigente y futura; la cancelacion
 * pasa por la ruta interna de la agenda, que libera materiales, sincroniza el calendario y publica el
 * evento como cualquier otra.
 */
class OnlineBookingPatientCancellationTest {

    /** Lunes 28/09/2026 08:00. */
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 28, 8, 0);

    private final UUID clinicId = UUID.randomUUID();
    private final UUID patientId = UUID.randomUUID();
    private final AppointmentRepositoryPort appointments = mock(AppointmentRepositoryPort.class);
    private final ManageAppointmentsUseCase agenda = mock(ManageAppointmentsUseCase.class);
    private final OnlineBookingService service = new OnlineBookingService(mock(ClinicScheduleService.class), appointments,
            mock(SlotHoldRepositoryPort.class), mock(OnlineBookingSettingsPort.class), mock(StaffValidatorPort.class),
            agenda, Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC));

    @Test
    void theirOwnUpcomingAppointmentIsCancelledThroughTheAgenda() {
        Appointment appointment = appointment(patientId, NOW.plusDays(1), AppointmentStatus.SCHEDULED);

        service.cancelByPatient(clinicId, appointment.getId(), patientId, "Tengo un viaje");

        verify(agenda).transitionStatus(appointment.getId(), clinicId, AppointmentStatus.CANCELLED,
                "Cancelada por el paciente por WhatsApp: Tengo un viaje", null);
    }

    @Test
    void withoutAReasonItStillSaysWhoCancelled() {
        Appointment appointment = appointment(patientId, NOW.plusDays(1), AppointmentStatus.CONFIRMED);

        service.cancelByPatient(clinicId, appointment.getId(), patientId, "  ");

        verify(agenda).transitionStatus(appointment.getId(), clinicId, AppointmentStatus.CANCELLED,
                "Cancelada por el paciente por WhatsApp", null);
    }

    @Test
    void someoneElsesPastOrAlreadyClosedAppointmentCannotBeCancelled() {
        Appointment otherPatients = appointment(UUID.randomUUID(), NOW.plusDays(1), AppointmentStatus.SCHEDULED);
        Appointment past = appointment(patientId, NOW.minusHours(1), AppointmentStatus.SCHEDULED);
        Appointment cancelled = appointment(patientId, NOW.plusDays(1), AppointmentStatus.CANCELLED);

        assertThrows(AppointmentNotCancellableException.class, () -> service.cancelByPatient(clinicId, otherPatients.getId(), patientId, null));
        assertThrows(AppointmentNotCancellableException.class, () -> service.cancelByPatient(clinicId, past.getId(), patientId, null));
        assertThrows(AppointmentNotCancellableException.class, () -> service.cancelByPatient(clinicId, cancelled.getId(), patientId, null));
        assertThrows(AppointmentNotCancellableException.class, () -> service.cancelByPatient(clinicId, UUID.randomUUID(), patientId, null));
        verify(agenda, never()).transitionStatus(any(UUID.class), any(), any(), anyString(), any());
    }

    private Appointment appointment(UUID owner, LocalDateTime start, AppointmentStatus status) {
        Appointment appointment = Appointment.builder().id(UUID.randomUUID()).clinicId(clinicId).patientId(owner)
                .doctorStaffId(UUID.randomUUID()).scheduledStart(start).scheduledEnd(start.plusMinutes(30)).status(status).build();
        when(appointments.findByIdAndClinicId(appointment.getId(), clinicId)).thenReturn(Optional.of(appointment));
        return appointment;
    }
}
