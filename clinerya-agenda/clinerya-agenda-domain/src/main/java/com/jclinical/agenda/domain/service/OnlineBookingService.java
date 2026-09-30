package com.jclinical.agenda.domain.service;

import com.jclinical.agenda.domain.model.Appointment;
import com.jclinical.agenda.domain.model.AppointmentStatus;
import com.jclinical.agenda.domain.model.BookableSlot;
import com.jclinical.agenda.domain.model.ClinicSchedule;
import com.jclinical.agenda.domain.model.SlotHold;
import com.jclinical.agenda.domain.ports.in.ManageAppointmentsUseCase;
import com.jclinical.agenda.domain.ports.in.ManageAppointmentsUseCase.CreateAppointmentCommand;
import com.jclinical.agenda.domain.ports.in.OnlineBookingUseCase;
import com.jclinical.agenda.domain.ports.out.AppointmentRepositoryPort;
import com.jclinical.agenda.domain.ports.out.OnlineBookingSettingsPort;
import com.jclinical.agenda.domain.ports.out.SlotHoldRepositoryPort;
import com.jclinical.agenda.domain.ports.out.StaffValidatorPort;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Cupos para citas en linea y apartado temporal. Un cupo es valido si cae dentro del horario de la
 * clinica, dura lo que la clinica configuro, empieza despues de la anticipacion minima del medico y
 * no choca con sus citas activas ni con un apartado vigente. Apartar revalida todo eso despues de
 * bloquear la agenda del medico, asi que dos pacientes nunca apartan el mismo horario.
 */
public class OnlineBookingService implements OnlineBookingUseCase {

    private final ClinicScheduleService clinicScheduleService;
    private final AppointmentRepositoryPort appointments;
    private final SlotHoldRepositoryPort holds;
    private final OnlineBookingSettingsPort settings;
    private final StaffValidatorPort staffValidator;
    private final ManageAppointmentsUseCase appointmentCreator;
    private final Clock clock;

    public OnlineBookingService(ClinicScheduleService clinicScheduleService, AppointmentRepositoryPort appointments,
                                SlotHoldRepositoryPort holds, OnlineBookingSettingsPort settings,
                                StaffValidatorPort staffValidator, ManageAppointmentsUseCase appointmentCreator,
                                Clock clock) {
        this.clinicScheduleService = clinicScheduleService;
        this.appointments = appointments;
        this.holds = holds;
        this.settings = settings;
        this.staffValidator = staffValidator;
        this.appointmentCreator = appointmentCreator;
        this.clock = clock;
    }

    @Override
    public List<BookableSlot> findAvailableSlots(UUID clinicId, UUID doctorStaffId, LocalDate from, int days, int limit) {
        return findAvailableSlots(clinicId, doctorStaffId, from, days, limit, null);
    }

    @Override
    public List<BookableSlot> findAvailableSlots(UUID clinicId, UUID doctorStaffId, LocalDate from, int days, int limit,
                                                 Integer durationMinutes) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime earliest = now.plusMinutes(settings.minLeadMinutes(clinicId, doctorStaffId));
        int slotMinutes = settings.slotMinutes(clinicId);
        int length = durationMinutes == null || durationMinutes <= 0 ? slotMinutes : durationMinutes;
        List<BookableSlot> available = new ArrayList<>();

        for (int offset = 0; offset < days && available.size() < limit; offset++) {
            LocalDate date = from.plusDays(offset);
            ClinicSchedule day = clinicScheduleService.getEffectiveDay(clinicId, date.getDayOfWeek());
            if (!day.isOpen() || day.getStartTime() == null || day.getEndTime() == null) {
                continue;
            }
            LocalDateTime dayStart = date.atTime(day.getStartTime());
            LocalDateTime dayEnd = date.atTime(day.getEndTime());
            List<Appointment> busy = appointments.findActiveByDoctorAndRange(doctorStaffId, clinicId, dayStart, dayEnd);
            List<SlotHold> held = holds.findActiveByDoctorAndRange(doctorStaffId, clinicId, dayStart, dayEnd, now);

            for (LocalDateTime start = dayStart;
                 !start.plusMinutes(length).isAfter(dayEnd) && available.size() < limit;
                 start = start.plusMinutes(slotMinutes)) {
                LocalDateTime end = start.plusMinutes(length);
                if (!start.isBefore(earliest) && isFree(start, end, busy, held)) {
                    available.add(new BookableSlot(start, end));
                }
            }
        }
        return available;
    }

    @Override
    public SlotHold holdSlot(HoldSlotCommand command) {
        if (staffValidator.findActiveDoctor(command.doctorStaffId(), command.clinicId()).isEmpty()) {
            throw new SlotUnavailableException("El médico ya no atiende en esta clínica.");
        }
        holds.lockDoctorSchedule(command.clinicId(), command.doctorStaffId());

        LocalDateTime now = LocalDateTime.now(clock);
        if (!isOfferable(command)) {
            throw new SlotUnavailableException("El horario elegido ya no está disponible.");
        }
        return holds.save(new SlotHold(UUID.randomUUID(), command.clinicId(), command.doctorStaffId(),
                command.start(), command.end(), now.plusMinutes(command.holdMinutes()), command.reference(),
                SlotHold.Status.ACTIVE, now));
    }

    @Override
    public void releaseHold(UUID clinicId, UUID holdId) {
        holds.findByIdAndClinicId(holdId, clinicId)
                .filter(hold -> hold.status() == SlotHold.Status.ACTIVE)
                .ifPresent(hold -> holds.save(hold.withStatus(SlotHold.Status.RELEASED)));
    }

    /**
     * El apartado se marca consumido antes de crear la cita para que la validacion de apartados de la
     * agenda no lo cuente como choque; si la agenda rechaza la cita, se restaura tal como estaba.
     */
    @Override
    public UUID bookHeldSlot(BookHeldSlotCommand command) {
        SlotHold found = holds.findByIdAndClinicId(command.holdId(), command.clinicId())
                .orElseThrow(() -> new SlotUnavailableException("El cupo apartado no existe."));
        holds.lockDoctorSchedule(found.clinicId(), found.doctorStaffId());

        SlotHold hold = holds.findByIdAndClinicId(found.id(), found.clinicId()).orElse(found);
        if (!hold.isActiveAt(LocalDateTime.now(clock))) {
            throw new SlotUnavailableException("El cupo apartado ya venció o fue liberado.");
        }
        holds.save(hold.withStatus(SlotHold.Status.CONSUMED));
        try {
            return appointmentCreator.createAppointment(null, hold.clinicId(), new CreateAppointmentCommand(
                    command.patientId(), hold.doctorStaffId(), null, null, null, List.of(),
                    hold.start(), hold.end(), command.reason(), null, false, command.serviceId())).getId();
        } catch (IllegalArgumentException | IllegalStateException rejected) {
            holds.save(hold);
            throw new SlotUnavailableException(rejected.getMessage());
        }
    }

    @Override
    public Optional<UUID> doctorStaffIdOfUser(UUID clinicId, UUID userId) {
        return staffValidator.staffIdOfUser(userId, clinicId);
    }

    @Override
    public void cancelByPatient(UUID clinicId, UUID appointmentId, UUID patientId, String reason) {
        Appointment appointment = appointments.findByIdAndClinicId(appointmentId, clinicId)
                .filter(found -> patientId != null && patientId.equals(found.getPatientId()))
                .filter(found -> found.getStatus() == AppointmentStatus.SCHEDULED || found.getStatus() == AppointmentStatus.CONFIRMED)
                .filter(found -> found.getScheduledStart().isAfter(LocalDateTime.now(clock)))
                .orElseThrow(() -> new AppointmentNotCancellableException("Esa cita ya no se puede cancelar por aquí."));
        String why = reason == null || reason.isBlank()
                ? "Cancelada por el paciente por WhatsApp"
                : "Cancelada por el paciente por WhatsApp: " + reason.trim();
        appointmentCreator.transitionStatus(appointment.getId(), clinicId, AppointmentStatus.CANCELLED, why, null);
    }

    @Override
    public List<UpcomingAppointment> upcomingAppointments(UUID clinicId, UUID patientId, int limit) {
        LocalDateTime now = LocalDateTime.now(clock);
        return appointments.findByPatientIdAndClinicId(patientId, clinicId).stream()
                .filter(appointment -> appointment.getStatus() == AppointmentStatus.SCHEDULED
                        || appointment.getStatus() == AppointmentStatus.CONFIRMED)
                .filter(appointment -> appointment.getScheduledStart().isAfter(now))
                .sorted(java.util.Comparator.comparing(Appointment::getScheduledStart))
                .limit(Math.max(0, limit))
                .map(appointment -> new UpcomingAppointment(appointment.getId(), appointment.getDoctorStaffId(),
                        appointment.getScheduledStart(), appointment.getScheduledEnd(),
                        appointment.getStatus() == AppointmentStatus.CONFIRMED))
                .toList();
    }

    @Override
    public VisitSummary visitSummary(UUID clinicId, UUID patientId) {
        List<Appointment> all = appointments.findByPatientIdAndClinicId(patientId, clinicId);
        java.util.Optional<UpcomingAppointment> next = upcomingAppointments(clinicId, patientId, 1).stream().findFirst();
        java.util.Optional<Appointment> lastAttended = all.stream()
                .filter(appointment -> appointment.getStatus() == AppointmentStatus.COMPLETED)
                .max(java.util.Comparator.comparing(Appointment::getScheduledStart));
        return new VisitSummary(
                next.map(UpcomingAppointment::start).orElse(null),
                next.map(UpcomingAppointment::doctorStaffId).orElse(null),
                lastAttended.map(Appointment::getScheduledStart).orElse(null),
                lastAttended.map(Appointment::getDoctorStaffId).orElse(null),
                count(all, AppointmentStatus.COMPLETED), count(all, AppointmentStatus.CANCELLED),
                count(all, AppointmentStatus.NO_SHOW));
    }

    private static int count(List<Appointment> all, AppointmentStatus status) {
        return (int) all.stream().filter(appointment -> appointment.getStatus() == status).count();
    }

    /**
     * Un cupo se puede apartar si es exactamente uno de los que se ofrecerian ese dia: misma rejilla,
     * mismas reglas. Asi un id de cupo viejo o manipulado nunca aparta un horario arbitrario.
     */
    private boolean isOfferable(HoldSlotCommand command) {
        LocalDate date = command.start().toLocalDate();
        int length = (int) java.time.Duration.between(command.start(), command.end()).toMinutes();
        return findAvailableSlots(command.clinicId(), command.doctorStaffId(), date, 1, Integer.MAX_VALUE, length).stream()
                .anyMatch(slot -> slot.start().equals(command.start()) && slot.end().equals(command.end()));
    }

    private static boolean isFree(LocalDateTime start, LocalDateTime end, List<Appointment> busy, List<SlotHold> held) {
        return busy.stream().noneMatch(appointment -> appointment.overlapsWith(start, end))
                && held.stream().noneMatch(hold -> hold.overlaps(start, end));
    }
}
