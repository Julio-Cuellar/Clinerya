package com.jclinical.agenda.domain.service;

import com.jclinical.agenda.domain.model.Appointment;
import com.jclinical.agenda.domain.model.AppointmentStatus;
import com.jclinical.agenda.domain.model.ClinicSchedule;
import com.jclinical.agenda.domain.model.SlotHold;
import com.jclinical.agenda.domain.ports.in.ManageAppointmentsUseCase;
import com.jclinical.agenda.domain.ports.in.ManageAppointmentsUseCase.CreateAppointmentCommand;
import com.jclinical.agenda.domain.ports.in.OnlineBookingUseCase.BookHeldSlotCommand;
import com.jclinical.agenda.domain.model.BookableSlot;
import com.jclinical.agenda.domain.ports.out.AppointmentRepositoryPort;
import com.jclinical.agenda.domain.ports.out.OnlineBookingSettingsPort;
import com.jclinical.agenda.domain.ports.out.SlotHoldRepositoryPort;
import com.jclinical.agenda.domain.ports.out.StaffValidatorPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Plan v2, S2: el agente busca horarios donde cabe el servicio completo. El inicio sigue la rejilla de
 * la clinica (30 min) pero cada cupo dura lo que el servicio; un cupo que chocaria con otra cita no se
 * ofrece. Al agendar un apartado, la cita lleva el servicio.
 */
class OnlineBookingServiceDurationTest {

    /** Lunes 28/09/2026 07:00. */
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 28, 7, 0);
    private static final LocalDate DAY = NOW.toLocalDate();

    private final UUID clinicId = UUID.randomUUID();
    private final UUID doctorId = UUID.randomUUID();
    private final ClinicScheduleService schedule = mock(ClinicScheduleService.class);
    private final AppointmentRepositoryPort appointments = mock(AppointmentRepositoryPort.class);
    private final SlotHoldRepositoryPort holds = mock(SlotHoldRepositoryPort.class);
    private final OnlineBookingSettingsPort settings = mock(OnlineBookingSettingsPort.class);
    private final ManageAppointmentsUseCase creator = mock(ManageAppointmentsUseCase.class);

    private OnlineBookingService service;

    @BeforeEach
    void setUp() {
        when(schedule.getEffectiveDay(eq(clinicId), any())).thenReturn(ClinicSchedule.builder().clinicId(clinicId)
                .dayOfWeek(DAY.getDayOfWeek()).open(true).startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(11, 0)).build());
        when(settings.slotMinutes(clinicId)).thenReturn(30);
        when(settings.minLeadMinutes(clinicId, doctorId)).thenReturn(0);
        when(holds.findActiveByDoctorAndRange(eq(doctorId), eq(clinicId), any(), any(), any())).thenReturn(List.of());
        service = new OnlineBookingService(schedule, appointments, holds, settings, mock(StaffValidatorPort.class), creator,
                Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
    }

    @Test
    void slotsLastAsLongAsTheServiceAndStartOnTheClinicGrid() {
        when(appointments.findActiveByDoctorAndRange(eq(doctorId), eq(clinicId), any(), any())).thenReturn(List.of(
                appointment(DAY.atTime(10, 0), DAY.atTime(10, 30))));

        List<BookableSlot> slots = service.findAvailableSlots(clinicId, doctorId, DAY, 1, 10, 45);

        assertEquals(List.of(new BookableSlot(DAY.atTime(9, 0), DAY.atTime(9, 45))), slots,
                "9:30 chocaria con la cita de 10:00 y 10:30 ya no cabe antes del cierre");
    }

    @Test
    void withoutDurationTheClinicSlotLengthIsUsedAsBefore() {
        when(appointments.findActiveByDoctorAndRange(eq(doctorId), eq(clinicId), any(), any())).thenReturn(List.of());

        assertEquals(4, service.findAvailableSlots(clinicId, doctorId, DAY, 1, 10).size());
    }

    @Test
    void bookingAHeldSlotForAServiceCarriesTheService() {
        UUID serviceId = UUID.randomUUID();
        SlotHold hold = new SlotHold(UUID.randomUUID(), clinicId, doctorId, DAY.atTime(9, 0), DAY.atTime(9, 45),
                NOW.plusHours(1), UUID.randomUUID(), SlotHold.Status.ACTIVE, NOW);
        when(holds.findByIdAndClinicId(hold.id(), clinicId)).thenReturn(Optional.of(hold));
        when(creator.createAppointment(eq(null), eq(clinicId), any()))
                .thenReturn(Appointment.builder().id(UUID.randomUUID()).status(AppointmentStatus.SCHEDULED).build());

        service.bookHeldSlot(new BookHeldSlotCommand(clinicId, hold.id(), UUID.randomUUID(), "WhatsApp", serviceId));

        ArgumentCaptor<CreateAppointmentCommand> command = ArgumentCaptor.forClass(CreateAppointmentCommand.class);
        verify(creator).createAppointment(eq(null), eq(clinicId), command.capture());
        assertEquals(serviceId, command.getValue().serviceId());
        assertEquals(DAY.atTime(9, 45), command.getValue().scheduledEnd());
    }

    private Appointment appointment(LocalDateTime start, LocalDateTime end) {
        return Appointment.builder().id(UUID.randomUUID()).clinicId(clinicId).doctorStaffId(doctorId)
                .scheduledStart(start).scheduledEnd(end).status(AppointmentStatus.SCHEDULED).build();
    }
}
