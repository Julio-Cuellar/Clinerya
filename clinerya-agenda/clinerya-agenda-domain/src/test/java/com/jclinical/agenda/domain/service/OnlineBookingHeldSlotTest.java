package com.jclinical.agenda.domain.service;

import com.jclinical.agenda.domain.model.Appointment;
import com.jclinical.agenda.domain.model.AppointmentStatus;
import com.jclinical.agenda.domain.model.SlotHold;
import com.jclinical.agenda.domain.ports.in.ManageAppointmentsUseCase;
import com.jclinical.agenda.domain.ports.in.ManageAppointmentsUseCase.CreateAppointmentCommand;
import com.jclinical.agenda.domain.ports.in.OnlineBookingUseCase.BookHeldSlotCommand;
import com.jclinical.agenda.domain.ports.in.OnlineBookingUseCase.SlotUnavailableException;
import com.jclinical.agenda.domain.ports.out.AppointmentRepositoryPort;
import com.jclinical.agenda.domain.ports.out.StaffValidatorPort;
import com.jclinical.agenda.domain.service.OnlineBookingServiceTest.FakeSettings;
import com.jclinical.agenda.domain.service.OnlineBookingServiceTest.InMemoryHolds;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Entrega 4: cuando el medico aprueba, el cupo apartado se vuelve cita. La agenda bloquea al medico,
 * exige que el apartado siga vigente y crea la cita con todas sus validaciones normales (horario,
 * choques). Si la agenda la rechaza, el apartado queda como estaba.
 */
class OnlineBookingHeldSlotTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 28, 8, 0);
    private static final LocalDateTime START = NOW.plusHours(2);

    private final UUID clinicId = UUID.randomUUID();
    private final UUID doctorId = UUID.randomUUID();
    private final UUID patientId = UUID.randomUUID();

    private final InMemoryHolds holds = new InMemoryHolds();
    private final ManageAppointmentsUseCase appointments = mock(ManageAppointmentsUseCase.class);
    private final StaffValidatorPort staff = mock(StaffValidatorPort.class);

    private OnlineBookingService service;

    @BeforeEach
    void setUp() {
        service = new OnlineBookingService(mock(ClinicScheduleService.class), mock(AppointmentRepositoryPort.class),
                holds, new FakeSettings(), staff, appointments, Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
    }

    @Test
    void bookingAHeldSlotCreatesTheAppointmentAndConsumesTheHold() {
        SlotHold hold = holds.save(hold(NOW.plusHours(24), SlotHold.Status.ACTIVE));
        UUID appointmentId = UUID.randomUUID();
        when(appointments.createAppointment(eq(null), eq(clinicId), any()))
                .thenReturn(Appointment.builder().id(appointmentId).status(AppointmentStatus.SCHEDULED).build());

        UUID created = service.bookHeldSlot(new BookHeldSlotCommand(clinicId, hold.id(), patientId, "Solicitud por WhatsApp"));

        assertEquals(appointmentId, created);
        assertEquals(SlotHold.Status.CONSUMED, holds.byId.get(hold.id()).status());
        assertEquals(List.of(doctorId), holds.lockedDoctors);
        ArgumentCaptor<CreateAppointmentCommand> command = ArgumentCaptor.forClass(CreateAppointmentCommand.class);
        verify(appointments).createAppointment(eq(null), eq(clinicId), command.capture());
        assertEquals(patientId, command.getValue().patientId());
        assertEquals(doctorId, command.getValue().doctorStaffId());
        assertEquals(START, command.getValue().scheduledStart());
        assertEquals(START.plusMinutes(30), command.getValue().scheduledEnd());
        assertEquals("Solicitud por WhatsApp", command.getValue().reason());
        assertNull(command.getValue().roomId());
    }

    @Test
    void aReleasedOrExpiredHoldCannotBeBooked() {
        SlotHold released = holds.save(hold(NOW.plusHours(24), SlotHold.Status.RELEASED));
        SlotHold expired = holds.save(hold(NOW.minusMinutes(1), SlotHold.Status.ACTIVE));

        assertThrows(SlotUnavailableException.class,
                () -> service.bookHeldSlot(new BookHeldSlotCommand(clinicId, released.id(), patientId, null)));
        assertThrows(SlotUnavailableException.class,
                () -> service.bookHeldSlot(new BookHeldSlotCommand(clinicId, expired.id(), patientId, null)));
        verify(appointments, never()).createAppointment(any(), any(), any());
    }

    @Test
    void aHoldFromAnotherClinicCannotBeBooked() {
        SlotHold hold = holds.save(hold(NOW.plusHours(24), SlotHold.Status.ACTIVE));

        assertThrows(SlotUnavailableException.class,
                () -> service.bookHeldSlot(new BookHeldSlotCommand(UUID.randomUUID(), hold.id(), patientId, null)));
    }

    @Test
    void whenTheAgendaRejectsTheAppointmentTheHoldStaysActive() {
        SlotHold hold = holds.save(hold(NOW.plusHours(24), SlotHold.Status.ACTIVE));
        when(appointments.createAppointment(eq(null), eq(clinicId), any()))
                .thenThrow(new IllegalStateException("El doctor ya tiene otra cita agendada en ese horario."));

        SlotUnavailableException error = assertThrows(SlotUnavailableException.class,
                () -> service.bookHeldSlot(new BookHeldSlotCommand(clinicId, hold.id(), patientId, null)));

        assertEquals("El doctor ya tiene otra cita agendada en ese horario.", error.getMessage());
        assertEquals(SlotHold.Status.ACTIVE, holds.byId.get(hold.id()).status());
    }

    @Test
    void theDoctorOfAUserIsResolvedByTheAgenda() {
        UUID userId = UUID.randomUUID();
        when(staff.staffIdOfUser(userId, clinicId)).thenReturn(Optional.of(doctorId));

        assertEquals(Optional.of(doctorId), service.doctorStaffIdOfUser(clinicId, userId));
    }

    private SlotHold hold(LocalDateTime expiresAt, SlotHold.Status status) {
        return new SlotHold(UUID.randomUUID(), clinicId, doctorId, START, START.plusMinutes(30), expiresAt,
                UUID.randomUUID(), status, NOW.minusHours(1));
    }
}
