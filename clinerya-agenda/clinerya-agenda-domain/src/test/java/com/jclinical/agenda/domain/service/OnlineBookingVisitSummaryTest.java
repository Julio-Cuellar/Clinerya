package com.jclinical.agenda.domain.service;

import com.jclinical.agenda.domain.model.Appointment;
import com.jclinical.agenda.domain.model.AppointmentStatus;
import com.jclinical.agenda.domain.ports.in.ManageAppointmentsUseCase;
import com.jclinical.agenda.domain.ports.in.OnlineBookingUseCase.VisitSummary;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Ficha breve del contacto en Chats: para reconocer a quien escribe basta su proxima cita, la ultima
 * que se atendio y cuantas atendio, cancelo o falto. Nada del expediente.
 */
class OnlineBookingVisitSummaryTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 29, 12, 0);

    private final UUID clinicId = UUID.randomUUID();
    private final UUID patientId = UUID.randomUUID();
    private final UUID doctorId = UUID.randomUUID();
    private final AppointmentRepositoryPort appointments = mock(AppointmentRepositoryPort.class);
    private final OnlineBookingService service = new OnlineBookingService(mock(ClinicScheduleService.class), appointments,
            mock(SlotHoldRepositoryPort.class), mock(OnlineBookingSettingsPort.class), mock(StaffValidatorPort.class),
            mock(ManageAppointmentsUseCase.class), Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC));

    @Test
    void itSummarizesTheNextTheLastAttendedAndTheCounts() {
        Appointment next = appointment(NOW.plusDays(3), AppointmentStatus.CONFIRMED);
        Appointment lastAttended = appointment(NOW.minusDays(10), AppointmentStatus.COMPLETED);
        when(appointments.findByPatientIdAndClinicId(patientId, clinicId)).thenReturn(List.of(
                appointment(NOW.minusDays(40), AppointmentStatus.COMPLETED),
                lastAttended,
                appointment(NOW.minusDays(5), AppointmentStatus.CANCELLED),
                appointment(NOW.minusDays(2), AppointmentStatus.NO_SHOW),
                appointment(NOW.plusDays(9), AppointmentStatus.SCHEDULED),
                next));

        VisitSummary summary = service.visitSummary(clinicId, patientId);

        assertEquals(next.getScheduledStart(), summary.nextStart());
        assertEquals(doctorId, summary.nextDoctorStaffId());
        assertEquals(lastAttended.getScheduledStart(), summary.lastAttendedStart());
        assertEquals(doctorId, summary.lastAttendedDoctorStaffId());
        assertEquals(2, summary.attended());
        assertEquals(1, summary.cancelled());
        assertEquals(1, summary.noShows());
    }

    @Test
    void aPatientWithoutAppointmentsHasAnEmptySummary() {
        when(appointments.findByPatientIdAndClinicId(patientId, clinicId)).thenReturn(List.of());

        VisitSummary summary = service.visitSummary(clinicId, patientId);

        assertNull(summary.nextStart());
        assertNull(summary.lastAttendedStart());
        assertEquals(0, summary.attended() + summary.cancelled() + summary.noShows());
    }

    private Appointment appointment(LocalDateTime start, AppointmentStatus status) {
        return Appointment.builder().id(UUID.randomUUID()).clinicId(clinicId).patientId(patientId).doctorStaffId(doctorId)
                .scheduledStart(start).scheduledEnd(start.plusMinutes(30)).status(status).build();
    }
}
