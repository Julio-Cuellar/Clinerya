package com.jclinical.agenda.domain.service;

import com.jclinical.agenda.domain.model.Appointment;
import com.jclinical.agenda.domain.model.AppointmentStatus;
import com.jclinical.agenda.domain.model.BookableSlot;
import com.jclinical.agenda.domain.model.ClinicSchedule;
import com.jclinical.agenda.domain.model.SlotHold;
import com.jclinical.agenda.domain.ports.in.OnlineBookingUseCase.HoldSlotCommand;
import com.jclinical.agenda.domain.ports.in.OnlineBookingUseCase.SlotUnavailableException;
import com.jclinical.agenda.domain.ports.out.AppointmentRepositoryPort;
import com.jclinical.agenda.domain.ports.out.OnlineBookingSettingsPort;
import com.jclinical.agenda.domain.ports.out.SlotHoldRepositoryPort;
import com.jclinical.agenda.domain.ports.out.StaffValidatorPort;
import com.jclinical.agenda.domain.ports.out.StaffValidatorPort.DoctorSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Cupos para citas pedidas por WhatsApp (entrega 3). Un cupo existe si cae dentro del horario de la
 * clinica, dura lo que la clinica configuro, respeta la anticipacion minima del medico y no choca con
 * sus citas activas ni con otro cupo apartado vigente. Apartar revalida todo eso bajo un bloqueo por
 * medico, para que dos pacientes nunca aparten el mismo horario.
 */
class OnlineBookingServiceTest {

    /** Lunes 28/09/2026 08:00. */
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 28, 8, 0);
    private static final LocalDate MONDAY = NOW.toLocalDate();

    private final UUID clinicId = UUID.randomUUID();
    private final UUID doctorId = UUID.randomUUID();

    private final ClinicScheduleService schedule = mock(ClinicScheduleService.class);
    private final AppointmentRepositoryPort appointments = mock(AppointmentRepositoryPort.class);
    private final StaffValidatorPort staff = mock(StaffValidatorPort.class);
    private final InMemoryHolds holds = new InMemoryHolds();
    private final FakeSettings settings = new FakeSettings();

    private OnlineBookingService service;
    private List<Appointment> doctorAppointments;

    @BeforeEach
    void setUp() {
        service = new OnlineBookingService(schedule, appointments, holds, settings, staff,
                Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
        for (DayOfWeek day : DayOfWeek.values()) {
            boolean weekday = day.getValue() <= 5;
            when(schedule.getEffectiveDay(clinicId, day)).thenReturn(ClinicSchedule.builder()
                    .clinicId(clinicId).dayOfWeek(day).open(weekday)
                    .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(11, 0)).build());
        }
        doctorAppointments = new ArrayList<>();
        when(appointments.findActiveByDoctorAndRange(eq(doctorId), eq(clinicId), any(), any()))
                .thenAnswer(invocation -> doctorAppointments);
        when(staff.findActiveDoctor(doctorId, clinicId)).thenReturn(Optional.of(new DoctorSnapshot(doctorId, "Dra. B")));
        settings.leadMinutes = 0;
    }

    // ---- disponibilidad ---------------------------------------------------------------------

    @Test
    void slotsFollowTheClinicHoursAndTheClinicSlotLength() {
        List<BookableSlot> slots = service.findAvailableSlots(clinicId, doctorId, MONDAY, 1, 10);

        assertEquals(List.of(slot(MONDAY, 9, 0), slot(MONDAY, 9, 30), slot(MONDAY, 10, 0), slot(MONDAY, 10, 30)), slots);
    }

    @Test
    void theClinicSlotLengthIsConfigurable() {
        settings.slotMinutes = 60;

        List<BookableSlot> slots = service.findAvailableSlots(clinicId, doctorId, MONDAY, 1, 10);

        assertEquals(List.of(new BookableSlot(at(MONDAY, 9, 0), at(MONDAY, 10, 0)),
                new BookableSlot(at(MONDAY, 10, 0), at(MONDAY, 11, 0))), slots);
    }

    @Test
    void closedDaysOfferNothing() {
        LocalDate saturday = MONDAY.plusDays(5);

        assertTrue(service.findAvailableSlots(clinicId, doctorId, saturday, 2, 10).isEmpty());
    }

    @Test
    void theDoctorsMinimumLeadTimeIsRespected() {
        settings.leadMinutes = 120; // ahora 08:00 -> nada antes de las 10:00

        List<BookableSlot> slots = service.findAvailableSlots(clinicId, doctorId, MONDAY, 1, 10);

        assertEquals(List.of(slot(MONDAY, 10, 0), slot(MONDAY, 10, 30)), slots);
    }

    @Test
    void slotsThatOverlapTheDoctorsAppointmentsAreNotOffered() {
        doctorAppointments.add(appointment(at(MONDAY, 9, 15), at(MONDAY, 9, 45)));

        List<BookableSlot> slots = service.findAvailableSlots(clinicId, doctorId, MONDAY, 1, 10);

        assertEquals(List.of(slot(MONDAY, 10, 0), slot(MONDAY, 10, 30)), slots);
    }

    @Test
    void anActiveHoldBlocksTheSlotButAnExpiredOneDoesNot() {
        holds.save(hold(at(MONDAY, 9, 0), NOW.plusHours(1), SlotHold.Status.ACTIVE));
        holds.save(hold(at(MONDAY, 10, 0), NOW.minusMinutes(1), SlotHold.Status.ACTIVE));

        List<BookableSlot> slots = service.findAvailableSlots(clinicId, doctorId, MONDAY, 1, 10);

        assertFalse(slots.contains(slot(MONDAY, 9, 0)), "apartado vigente");
        assertTrue(slots.contains(slot(MONDAY, 10, 0)), "apartado vencido ya no cuenta");
    }

    @Test
    void theSearchStopsAtTheLimitAndSpansSeveralDays() {
        List<BookableSlot> slots = service.findAvailableSlots(clinicId, doctorId, MONDAY, 3, 6);

        assertEquals(6, slots.size());
        assertEquals(slot(MONDAY.plusDays(1), 9, 30), slots.get(5));
    }

    // ---- apartado ---------------------------------------------------------------------------

    @Test
    void holdingAFreeSlotKeepsItForTheGivenTime() {
        UUID reference = UUID.randomUUID();

        SlotHold hold = service.holdSlot(new HoldSlotCommand(clinicId, doctorId, at(MONDAY, 9, 0), at(MONDAY, 9, 30), reference, 24 * 60));

        assertEquals(SlotHold.Status.ACTIVE, hold.status());
        assertEquals(NOW.plusHours(24), hold.expiresAt());
        assertEquals(reference, hold.reference());
        assertEquals(List.of(doctorId), holds.lockedDoctors, "se bloquea la agenda del medico antes de revalidar");
        assertFalse(service.findAvailableSlots(clinicId, doctorId, MONDAY, 1, 10).contains(slot(MONDAY, 9, 0)));
    }

    @Test
    void aSlotAlreadyHeldCannotBeHeldAgain() {
        service.holdSlot(new HoldSlotCommand(clinicId, doctorId, at(MONDAY, 9, 0), at(MONDAY, 9, 30), UUID.randomUUID(), 60));

        assertThrows(SlotUnavailableException.class, () -> service.holdSlot(
                new HoldSlotCommand(clinicId, doctorId, at(MONDAY, 9, 0), at(MONDAY, 9, 30), UUID.randomUUID(), 60)));
    }

    @Test
    void aSlotTakenByAnAppointmentCannotBeHeld() {
        doctorAppointments.add(appointment(at(MONDAY, 9, 0), at(MONDAY, 9, 30)));

        assertThrows(SlotUnavailableException.class, () -> service.holdSlot(
                new HoldSlotCommand(clinicId, doctorId, at(MONDAY, 9, 0), at(MONDAY, 9, 30), UUID.randomUUID(), 60)));
    }

    @Test
    void aSlotOutsideTheRulesCannotBeHeld() {
        settings.leadMinutes = 120;

        assertThrows(SlotUnavailableException.class, () -> service.holdSlot(
                new HoldSlotCommand(clinicId, doctorId, at(MONDAY, 9, 0), at(MONDAY, 9, 30), UUID.randomUUID(), 60)),
                "antes de la anticipacion minima");
        assertThrows(SlotUnavailableException.class, () -> service.holdSlot(
                new HoldSlotCommand(clinicId, doctorId, at(MONDAY, 18, 0), at(MONDAY, 18, 30), UUID.randomUUID(), 60)),
                "fuera del horario de la clinica");
    }

    @Test
    void aDoctorWhoNoLongerAttendsCannotBeHeld() {
        when(staff.findActiveDoctor(doctorId, clinicId)).thenReturn(Optional.empty());

        assertThrows(SlotUnavailableException.class, () -> service.holdSlot(
                new HoldSlotCommand(clinicId, doctorId, at(MONDAY, 9, 0), at(MONDAY, 9, 30), UUID.randomUUID(), 60)));
    }

    @Test
    void releasingAHoldFreesTheSlot() {
        SlotHold hold = service.holdSlot(
                new HoldSlotCommand(clinicId, doctorId, at(MONDAY, 9, 0), at(MONDAY, 9, 30), UUID.randomUUID(), 60));

        service.releaseHold(clinicId, hold.id());

        assertEquals(SlotHold.Status.RELEASED, holds.byId.get(hold.id()).status());
        assertTrue(service.findAvailableSlots(clinicId, doctorId, MONDAY, 1, 10).contains(slot(MONDAY, 9, 0)));
    }

    // ---- utilidades -------------------------------------------------------------------------

    private static LocalDateTime at(LocalDate date, int hour, int minute) {
        return date.atTime(hour, minute);
    }

    private static BookableSlot slot(LocalDate date, int hour, int minute) {
        LocalDateTime start = at(date, hour, minute);
        return new BookableSlot(start, start.plusMinutes(30));
    }

    private Appointment appointment(LocalDateTime start, LocalDateTime end) {
        return Appointment.builder().id(UUID.randomUUID()).clinicId(clinicId).doctorStaffId(doctorId)
                .scheduledStart(start).scheduledEnd(end).status(AppointmentStatus.SCHEDULED).build();
    }

    private SlotHold hold(LocalDateTime start, LocalDateTime expiresAt, SlotHold.Status status) {
        return new SlotHold(UUID.randomUUID(), clinicId, doctorId, start, start.plusMinutes(30), expiresAt,
                UUID.randomUUID(), status, NOW.minusHours(1));
    }

    static final class FakeSettings implements OnlineBookingSettingsPort {
        int slotMinutes = OnlineBookingSettingsPort.DEFAULT_SLOT_MINUTES;
        int leadMinutes = OnlineBookingSettingsPort.DEFAULT_MIN_LEAD_MINUTES;

        @Override
        public int slotMinutes(UUID clinicId) {
            return slotMinutes;
        }

        @Override
        public int minLeadMinutes(UUID clinicId, UUID doctorStaffId) {
            return leadMinutes;
        }
    }

    static final class InMemoryHolds implements SlotHoldRepositoryPort {
        final Map<UUID, SlotHold> byId = new HashMap<>();
        final List<UUID> lockedDoctors = new ArrayList<>();

        @Override
        public SlotHold save(SlotHold hold) {
            byId.put(hold.id(), hold);
            return hold;
        }

        @Override
        public Optional<SlotHold> findByIdAndClinicId(UUID holdId, UUID clinicId) {
            return Optional.ofNullable(byId.get(holdId)).filter(hold -> hold.clinicId().equals(clinicId));
        }

        @Override
        public List<SlotHold> findActiveByDoctorAndRange(UUID doctorStaffId, UUID clinicId, LocalDateTime from,
                                                         LocalDateTime to, LocalDateTime now) {
            return byId.values().stream()
                    .filter(hold -> hold.doctorStaffId().equals(doctorStaffId) && hold.clinicId().equals(clinicId))
                    .filter(hold -> hold.isActiveAt(now))
                    .filter(hold -> hold.start().isBefore(to) && hold.end().isAfter(from))
                    .toList();
        }

        @Override
        public void lockDoctorSchedule(UUID clinicId, UUID doctorStaffId) {
            lockedDoctors.add(doctorStaffId);
        }
    }
}
