package com.jclinical.agenda.domain.service;

import com.jclinical.agenda.domain.model.Appointment;
import com.jclinical.agenda.domain.model.AppointmentStatus;
import com.jclinical.agenda.domain.ports.in.ManageAppointmentsUseCase;
import com.jclinical.agenda.domain.ports.in.OnlineBookingUseCase.UpcomingAppointment;
import com.jclinical.agenda.domain.ports.out.AppointmentRepositoryPort;
import com.jclinical.agenda.domain.ports.out.OnlineBookingSettingsPort;
import com.jclinical.agenda.domain.ports.out.SlotHoldRepositoryPort;
import com.jclinical.agenda.domain.ports.out.StaffValidatorPort;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * "¿Cuándo es mi cita?" por WhatsApp: las proximas citas vigentes de un paciente, por la ruta interna
 * de la automatizacion (quien pregunta es el paciente, no un usuario del personal).
 */
class OnlineBookingUpcomingAppointmentsTest {

    /** Lunes 28/09/2026 08:00. */
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 28, 8, 0);

    private final UUID clinicId = UUID.randomUUID();
    private final UUID patientId = UUID.randomUUID();
    private final UUID doctorId = UUID.randomUUID();
    private final AppointmentRepositoryPort appointments = mock(AppointmentRepositoryPort.class);
    private final OnlineBookingService service = new OnlineBookingService(mock(ClinicScheduleService.class), appointments,
            mock(SlotHoldRepositoryPort.class), mock(OnlineBookingSettingsPort.class), mock(StaffValidatorPort.class),
            mock(ManageAppointmentsUseCase.class), Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC));

    @Test
    void onlyFutureScheduledOrConfirmedAppointmentsSoonestFirst() {
        Appointment tuesdayConfirmed = appointment(NOW.plusDays(1).withHour(10), AppointmentStatus.CONFIRMED);
        Appointment todayScheduled = appointment(NOW.withHour(12), AppointmentStatus.SCHEDULED);
        when(appointments.findByPatientIdAndClinicId(patientId, clinicId)).thenReturn(List.of(
                tuesdayConfirmed,
                appointment(NOW.minusDays(1), AppointmentStatus.SCHEDULED),
                appointment(NOW.plusDays(2), AppointmentStatus.CANCELLED),
                appointment(NOW.plusDays(3), AppointmentStatus.NO_SHOW),
                todayScheduled));

        List<UpcomingAppointment> upcoming = service.upcomingAppointments(clinicId, patientId, 5);

        assertEquals(List.of(upcoming(todayScheduled, false), upcoming(tuesdayConfirmed, true)), upcoming);
    }

    @Test
    void theLimitIsRespected() {
        when(appointments.findByPatientIdAndClinicId(patientId, clinicId)).thenReturn(List.of(
                appointment(NOW.plusDays(1), AppointmentStatus.SCHEDULED),
                appointment(NOW.plusDays(2), AppointmentStatus.SCHEDULED)));

        assertEquals(1, service.upcomingAppointments(clinicId, patientId, 1).size());
    }

    @Test
    void aPatientWithoutAppointmentsHasNone() {
        when(appointments.findByPatientIdAndClinicId(patientId, clinicId)).thenReturn(List.of());

        assertTrue(service.upcomingAppointments(clinicId, patientId, 5).isEmpty());
    }

    private Appointment appointment(LocalDateTime start, AppointmentStatus status) {
        return Appointment.builder().id(UUID.randomUUID()).clinicId(clinicId).patientId(patientId).doctorStaffId(doctorId)
                .scheduledStart(start).scheduledEnd(start.plusMinutes(30)).status(status).build();
    }

    private static UpcomingAppointment upcoming(Appointment appointment, boolean confirmed) {
        return new UpcomingAppointment(appointment.getId(), appointment.getDoctorStaffId(), appointment.getScheduledStart(),
                appointment.getScheduledEnd(), confirmed);
    }
}
