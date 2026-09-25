package com.jclinical.agenda.domain.service;

import com.jclinical.agenda.domain.model.Appointment;
import com.jclinical.agenda.domain.model.AppointmentStatus;
import com.jclinical.agenda.domain.model.ClinicSchedule;
import com.jclinical.agenda.domain.model.SlotHold;
import com.jclinical.agenda.domain.ports.in.ManageAppointmentsUseCase.CreateAppointmentCommand;
import com.jclinical.agenda.domain.ports.out.AppointmentRepositoryPort;
import com.jclinical.agenda.domain.ports.out.PatientValidatorPort;
import com.jclinical.agenda.domain.ports.out.QuotationValidatorPort;
import com.jclinical.agenda.domain.ports.out.StaffValidatorPort;
import com.jclinical.agenda.domain.ports.out.StaffValidatorPort.DoctorSnapshot;
import com.jclinical.core.events.DomainEventPublisherPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Decision de la entrega 3: el cupo que un paciente aparto por WhatsApp tambien queda bloqueado para
 * el personal. Mientras el apartado este vigente, la agenda no deja agendar ni reprogramar encima;
 * vencido o liberado, deja de estorbar.
 */
class AppointmentHoldConflictTest {

    private final UUID clinicId = UUID.randomUUID();
    private final UUID doctorId = UUID.randomUUID();
    private final UUID patientId = UUID.randomUUID();
    private final LocalDateTime start = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);

    private final AppointmentRepositoryPort appointments = mock(AppointmentRepositoryPort.class);
    private final ClinicScheduleService schedule = mock(ClinicScheduleService.class);
    private final PatientValidatorPort patients = mock(PatientValidatorPort.class);
    private final StaffValidatorPort staff = mock(StaffValidatorPort.class);
    private final OnlineBookingServiceTest.InMemoryHolds holds = new OnlineBookingServiceTest.InMemoryHolds();

    private AppointmentService service;

    @BeforeEach
    void setUp() {
        service = new AppointmentService(appointments, schedule, patients, staff, mock(QuotationValidatorPort.class),
                mock(MaterialReservationSchedulingService.class), mock(DomainEventPublisherPort.class),
                null, null, (clinic, user, permission) -> true, holds);
        when(patients.existsByIdAndClinicId(patientId, clinicId)).thenReturn(true);
        when(staff.findActiveDoctor(doctorId, clinicId)).thenReturn(Optional.of(new DoctorSnapshot(doctorId, "Dra. B")));
        when(schedule.getEffectiveDay(any(), any())).thenAnswer(invocation -> ClinicSchedule.builder()
                .clinicId(clinicId).dayOfWeek(invocation.getArgument(1)).open(true)
                .startTime(LocalTime.of(8, 0)).endTime(LocalTime.of(20, 0)).build());
        when(appointments.findActiveByDoctorAndRange(any(), any(), any(), any())).thenReturn(List.of());
        when(appointments.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void staffCannotBookOverAnActiveHold() {
        holds.save(hold(start, LocalDateTime.now().plusHours(24), SlotHold.Status.ACTIVE));

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> service.createAppointment(clinicId,
                new CreateAppointmentCommand(patientId, doctorId, null, null, start.plusMinutes(15), start.plusMinutes(45), "Revision", null)));

        assertTrue(error.getMessage().contains("apartado"), error.getMessage());
    }

    @Test
    void anExpiredOrReleasedHoldNoLongerBlocks() {
        holds.save(hold(start, LocalDateTime.now().minusMinutes(1), SlotHold.Status.ACTIVE));
        holds.save(hold(start, LocalDateTime.now().plusHours(24), SlotHold.Status.RELEASED));

        Appointment created = service.createAppointment(clinicId,
                new CreateAppointmentCommand(patientId, doctorId, null, null, start, start.plusMinutes(30), "Revision", null));

        assertEquals(start, created.getScheduledStart());
    }

    @Test
    void staffCannotRescheduleOntoAnActiveHold() {
        holds.save(hold(start, LocalDateTime.now().plusHours(24), SlotHold.Status.ACTIVE));
        Appointment existing = Appointment.builder().id(UUID.randomUUID()).clinicId(clinicId).patientId(patientId)
                .doctorStaffId(doctorId).scheduledStart(start.plusDays(1)).scheduledEnd(start.plusDays(1).plusMinutes(30))
                .status(AppointmentStatus.SCHEDULED).build();
        when(appointments.findByIdAndClinicId(existing.getId(), clinicId)).thenReturn(Optional.of(existing));

        assertThrows(IllegalStateException.class, () -> service.rescheduleAppointment(
                existing.getId(), clinicId, start, start.plusMinutes(30)));
    }

    private SlotHold hold(LocalDateTime holdStart, LocalDateTime expiresAt, SlotHold.Status status) {
        return new SlotHold(UUID.randomUUID(), clinicId, doctorId, holdStart, holdStart.plusMinutes(30), expiresAt,
                UUID.randomUUID(), status, LocalDateTime.now());
    }
}
