package com.jclinical.agenda.domain.service;

import com.jclinical.agenda.domain.model.Appointment;
import com.jclinical.agenda.domain.model.AppointmentStatus;
import com.jclinical.agenda.domain.model.ClinicSchedule;
import com.jclinical.agenda.domain.ports.in.ManageAppointmentsUseCase;
import com.jclinical.agenda.domain.ports.out.AppointmentRepositoryPort;
import com.jclinical.agenda.domain.ports.out.SlotHoldRepositoryPort;
import com.jclinical.agenda.domain.ports.out.PatientValidatorPort;
import com.jclinical.agenda.domain.ports.out.QuotationValidatorPort;
import com.jclinical.agenda.domain.ports.out.QuotationValidatorPort.AcceptedQuotationSnapshot;
import com.jclinical.agenda.domain.ports.out.RoomBlockRepositoryPort;
import com.jclinical.agenda.domain.ports.out.RoomValidatorPort;
import com.jclinical.agenda.domain.ports.out.StaffValidatorPort;
import com.jclinical.agenda.domain.ports.out.StaffValidatorPort.DoctorSnapshot;
import com.jclinical.core.events.AppointmentCancelledEvent;
import com.jclinical.core.events.AppointmentDeletedEvent;
import com.jclinical.core.events.AppointmentRescheduledEvent;
import com.jclinical.core.events.AppointmentScheduledEvent;
import com.jclinical.core.events.DomainEventPublisherPort;
import com.jclinical.core.events.DomainEventRoutingKeys;
import com.jclinical.core.events.MaterialReservationReleasedEvent;
import com.jclinical.core.security.ClinicAccessDeniedException;
import com.jclinical.core.security.StaffPermission;
import com.jclinical.core.security.StaffPermissionCheckerPort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class AppointmentService implements ManageAppointmentsUseCase {

    private final AppointmentRepositoryPort appointmentRepository;
    private final ClinicScheduleService clinicScheduleService;
    private final PatientValidatorPort patientValidator;
    private final StaffValidatorPort staffValidator;
    private final QuotationValidatorPort quotationValidator;
    private final MaterialReservationSchedulingService materialReservationSchedulingService;
    private final DomainEventPublisherPort eventPublisher;
    private final RoomBlockRepositoryPort roomBlockRepository;
    private final RoomValidatorPort roomValidator;
    private final StaffPermissionCheckerPort permissionChecker;
    private final SlotHoldRepositoryPort slotHoldRepository;

    public AppointmentService(
            AppointmentRepositoryPort appointmentRepository,
            ClinicScheduleService clinicScheduleService,
            PatientValidatorPort patientValidator,
            StaffValidatorPort staffValidator,
            QuotationValidatorPort quotationValidator,
            MaterialReservationSchedulingService materialReservationSchedulingService,
            DomainEventPublisherPort eventPublisher,
            StaffPermissionCheckerPort permissionChecker) {
        this(appointmentRepository, clinicScheduleService, patientValidator, staffValidator, quotationValidator,
                materialReservationSchedulingService, eventPublisher, null, null, permissionChecker);
    }

    public AppointmentService(
            AppointmentRepositoryPort appointmentRepository,
            ClinicScheduleService clinicScheduleService,
            PatientValidatorPort patientValidator,
            StaffValidatorPort staffValidator,
            QuotationValidatorPort quotationValidator,
            MaterialReservationSchedulingService materialReservationSchedulingService,
            DomainEventPublisherPort eventPublisher,
            RoomBlockRepositoryPort roomBlockRepository,
            RoomValidatorPort roomValidator,
            StaffPermissionCheckerPort permissionChecker) {
        this(appointmentRepository, clinicScheduleService, patientValidator, staffValidator, quotationValidator,
                materialReservationSchedulingService, eventPublisher, roomBlockRepository, roomValidator, permissionChecker, null);
    }

    public AppointmentService(
            AppointmentRepositoryPort appointmentRepository,
            ClinicScheduleService clinicScheduleService,
            PatientValidatorPort patientValidator,
            StaffValidatorPort staffValidator,
            QuotationValidatorPort quotationValidator,
            MaterialReservationSchedulingService materialReservationSchedulingService,
            DomainEventPublisherPort eventPublisher,
            RoomBlockRepositoryPort roomBlockRepository,
            RoomValidatorPort roomValidator,
            StaffPermissionCheckerPort permissionChecker,
            SlotHoldRepositoryPort slotHoldRepository) {
        this.slotHoldRepository = slotHoldRepository;
        this.appointmentRepository = appointmentRepository;
        this.clinicScheduleService = clinicScheduleService;
        this.patientValidator = patientValidator;
        this.staffValidator = staffValidator;
        this.quotationValidator = quotationValidator;
        this.materialReservationSchedulingService = materialReservationSchedulingService;
        this.eventPublisher = eventPublisher;
        this.roomBlockRepository = roomBlockRepository;
        this.roomValidator = roomValidator;
        this.permissionChecker = permissionChecker;
    }

    /**
     * Aplica el permiso solo cuando hay un usuario actuando (llamada desde el adaptador web).
     * {@code actingUserId == null} identifica una ruta interna de confianza (sincronizacion de
     * calendario externo, seeders) y omite el control.
     */
    private void requirePermission(UUID clinicId, UUID actingUserId, StaffPermission permission, String deniedMessage) {
        if (actingUserId == null) {
            return;
        }
        if (!permissionChecker.hasPermission(clinicId, actingUserId, permission)) {
            throw new ClinicAccessDeniedException(deniedMessage);
        }
    }

    @Override
    public Appointment createAppointment(UUID actingUserId, UUID clinicId, CreateAppointmentCommand command) {
        requirePermission(clinicId, actingUserId, StaffPermission.CREATE_APPOINTMENTS,
                "No tienes permiso para crear citas en esta clinica.");
        List<UUID> quotationItemIds = normalizeQuotationItemIds(command.quotationItemIds(), command.quotationItemId());
        if (command.patientId() != null) {
            if (!patientValidator.existsByIdAndClinicId(command.patientId(), clinicId)) {
                throw new IllegalArgumentException("El paciente no existe en esta clínica.");
            }
        }

        if (command.patientId() == null && (command.quotationId() != null || !quotationItemIds.isEmpty())) {
            throw new IllegalArgumentException("No se puede asociar una cotización a una cita sin paciente.");
        }

        staffValidator.findActiveDoctor(command.doctorStaffId(), clinicId)
                .orElseThrow(() -> new IllegalArgumentException("El doctor indicado no existe o no está activo en esta clínica."));

        validateTimeRange(command.scheduledStart(), command.scheduledEnd());
        validateFutureStart(command.scheduledStart(), command.externalImport());

        // Los eventos importados desde el calendario externo del doctor (ej. bloqueos
        // personales de todo el día) no son citas reales de consultorio: no tiene sentido
        // exigirles caber en el horario de atención ni bloquearlos por traslape.
        if (!command.externalImport()) {
            validateWithinClinicSchedule(clinicId, command.scheduledStart(), command.scheduledEnd());
        }

        if (command.quotationId() != null) {
            validateQuotationLink(command.quotationId(), quotationItemIds, command.patientId(), clinicId);
        } else if (!quotationItemIds.isEmpty()) {
            throw new IllegalArgumentException("No se puede indicar una partida de cotización sin indicar la cotización.");
        }

        if (!command.externalImport()) {
            validateNoDoctorOverlap(command.doctorStaffId(), clinicId, command.scheduledStart(), command.scheduledEnd(), null);
            validateNoActiveHold(command.doctorStaffId(), clinicId, command.scheduledStart(), command.scheduledEnd());
            if (command.roomId() != null) {
                validateActiveRoom(command.roomId(), clinicId);
                validateNoRoomOverlap(command.roomId(), clinicId, command.scheduledStart(), command.scheduledEnd(), null);
                validateNoRoomBlockOverlap(command.roomId(), clinicId, command.scheduledStart(), command.scheduledEnd());
            }
        }

        Appointment appointment = Appointment.builder()
                .id(UUID.randomUUID())
                .clinicId(clinicId)
                .patientId(command.patientId())
                .doctorStaffId(command.doctorStaffId())
                .roomId(command.roomId())
                .quotationId(command.quotationId())
                .quotationItemId(quotationItemIds.isEmpty() ? null : quotationItemIds.get(0))
                .quotationItemIds(quotationItemIds)
                .scheduledStart(command.scheduledStart())
                .scheduledEnd(command.scheduledEnd())
                .reason(command.reason())
                .notes(command.notes())
                .status(AppointmentStatus.SCHEDULED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Appointment saved = appointmentRepository.save(appointment);

        eventPublisher.publish(DomainEventRoutingKeys.APPOINTMENT_SCHEDULED, new AppointmentScheduledEvent(
                UUID.randomUUID(), clinicId, saved.getId(), saved.getDoctorStaffId(),
                saved.getScheduledStart(), saved.getScheduledEnd(), LocalDateTime.now(), saved.getPatientId()));

        materialReservationSchedulingService.processReservation(saved);

        return saved;
    }

    @Override
    public List<Appointment> createAppointmentSeries(UUID actingUserId, UUID clinicId, CreateAppointmentSeriesCommand command) {
        requirePermission(clinicId, actingUserId, StaffPermission.CREATE_APPOINTMENTS,
                "No tienes permiso para crear citas en esta clinica.");
        if (command.repeatCount() <= 0 || command.repeatCount() > 52) {
            throw new IllegalArgumentException("El número de repeticiones debe ser entre 1 y 52.");
        }
        UUID seriesId = UUID.randomUUID();
        List<Appointment> createdList = new java.util.ArrayList<>();

        LocalDateTime currentStart = command.firstScheduledStart();
        LocalDateTime currentEnd = command.firstScheduledEnd();

        for (int i = 0; i < command.repeatCount(); i++) {
            CreateAppointmentCommand singleCommand = new CreateAppointmentCommand(
                    command.patientId(),
                    command.doctorStaffId(),
                    command.roomId(),
                    command.quotationId(),
                    command.quotationItemId(),
                    command.quotationItemIds(),
                    currentStart,
                    currentEnd,
                    command.reason(),
                    command.notes(),
                    false
            );
            Appointment app = createAppointment(clinicId, singleCommand);
            app.setSeriesId(seriesId);
            appointmentRepository.save(app);
            createdList.add(app);

            // Increment based on frequency
            switch (command.frequency()) {
                case DAILY -> {
                    currentStart = currentStart.plusDays(1);
                    currentEnd = currentEnd.plusDays(1);
                }
                case WEEKLY -> {
                    currentStart = currentStart.plusWeeks(1);
                    currentEnd = currentEnd.plusWeeks(1);
                }
                case BIWEEKLY -> {
                    currentStart = currentStart.plusWeeks(2);
                    currentEnd = currentEnd.plusWeeks(2);
                }
                case MONTHLY -> {
                    currentStart = currentStart.plusMonths(1);
                    currentEnd = currentEnd.plusMonths(1);
                }
            }
        }
        return createdList;
    }

    @Override
    public Appointment rescheduleAppointment(UUID actingUserId, UUID appointmentId, UUID clinicId, LocalDateTime newStart, LocalDateTime newEnd, boolean externalImport) {
        requirePermission(clinicId, actingUserId, StaffPermission.EDIT_APPOINTMENTS,
                "No tienes permiso para reprogramar citas en esta clinica.");
        Appointment appointment = getAppointment(appointmentId, clinicId);
        validateTimeRange(newStart, newEnd);
        validateFutureStart(newStart, externalImport);
        if (!externalImport) {
            validateWithinClinicSchedule(clinicId, newStart, newEnd);
            validateNoDoctorOverlap(appointment.getDoctorStaffId(), clinicId, newStart, newEnd, appointmentId);
            validateNoActiveHold(appointment.getDoctorStaffId(), clinicId, newStart, newEnd);
            if (appointment.getRoomId() != null) {
                validateActiveRoom(appointment.getRoomId(), clinicId);
                validateNoRoomOverlap(appointment.getRoomId(), clinicId, newStart, newEnd, appointmentId);
                validateNoRoomBlockOverlap(appointment.getRoomId(), clinicId, newStart, newEnd);
            }
        }
        LocalDateTime previousStart = appointment.getScheduledStart();
        LocalDateTime previousEnd = appointment.getScheduledEnd();
        appointment.reschedule(newStart, newEnd);
        Appointment saved = appointmentRepository.save(appointment);

        eventPublisher.publish(DomainEventRoutingKeys.APPOINTMENT_RESCHEDULED, new AppointmentRescheduledEvent(
                UUID.randomUUID(), clinicId, appointmentId, newStart, newEnd, LocalDateTime.now(),
                saved.getPatientId(), saved.getDoctorStaffId(), previousStart, previousEnd));

        materialReservationSchedulingService.processReservation(saved);

        return saved;
    }

    @Override
    public Appointment transitionStatus(UUID actingUserId, UUID appointmentId, UUID clinicId, AppointmentStatus targetStatus, String cancellationReason, UUID cancelledByUserId) {
        StaffPermission required = targetStatus == AppointmentStatus.CANCELLED
                ? StaffPermission.CANCEL_APPOINTMENTS
                : StaffPermission.EDIT_APPOINTMENTS;
        requirePermission(clinicId, actingUserId, required,
                "No tienes permiso para cambiar el estado de las citas de esta clinica.");
        Appointment appointment = getAppointment(appointmentId, clinicId);
        boolean hadMaterialsReserved = appointment.isMaterialsReserved();

        if (targetStatus == AppointmentStatus.CANCELLED) {
            appointment.cancel(cancellationReason, cancelledByUserId);
        } else {
            appointment.transitionTo(targetStatus);
        }

        boolean shouldReleaseMaterials = hadMaterialsReserved && releasesMaterialReservation(targetStatus);
        if (shouldReleaseMaterials) {
            appointment.setMaterialsReserved(false);
        }
        Appointment saved = appointmentRepository.save(appointment);

        if (shouldReleaseMaterials) {
            eventPublisher.publish(DomainEventRoutingKeys.MATERIAL_RESERVATION_RELEASED, new MaterialReservationReleasedEvent(
                    UUID.randomUUID(), clinicId, appointmentId, LocalDateTime.now()));
        }

        if (targetStatus == AppointmentStatus.CANCELLED) {
            eventPublisher.publish(DomainEventRoutingKeys.APPOINTMENT_CANCELLED, new AppointmentCancelledEvent(
                    UUID.randomUUID(), clinicId, appointmentId, LocalDateTime.now(),
                    saved.getPatientId(), saved.getDoctorStaffId(), saved.getScheduledStart(), cancellationReason));
        }

        return saved;
    }

    private boolean releasesMaterialReservation(AppointmentStatus status) {
        return status == AppointmentStatus.COMPLETED
                || status == AppointmentStatus.CANCELLED
                || status == AppointmentStatus.NO_SHOW;
    }

    @Override
    public Appointment attachExternalCalendarEvent(UUID appointmentId, UUID clinicId, String externalCalendarEventId) {
        Appointment appointment = getAppointment(appointmentId, clinicId);
        appointment.setExternalCalendarEventId(externalCalendarEventId);
        return appointmentRepository.save(appointment);
    }

    @Override
    public void deleteAppointment(UUID actingUserId, UUID appointmentId, UUID clinicId) {
        requirePermission(clinicId, actingUserId, StaffPermission.CANCEL_APPOINTMENTS,
                "No tienes permiso para eliminar citas en esta clinica.");
        Appointment appointment = getAppointment(appointmentId, clinicId);
        appointmentRepository.delete(appointment);

        eventPublisher.publish(DomainEventRoutingKeys.APPOINTMENT_DELETED, new AppointmentDeletedEvent(
                UUID.randomUUID(), clinicId, appointmentId, appointment.getDoctorStaffId(),
                appointment.getExternalCalendarEventId(), LocalDateTime.now()));
    }

    @Override
    public Appointment getAppointment(UUID appointmentId, UUID clinicId) {
        return appointmentRepository.findByIdAndClinicId(appointmentId, clinicId)
                .orElseThrow(() -> new IllegalArgumentException("La cita no existe en esta clínica."));
    }

    @Override
    public List<Appointment> listByClinicRange(UUID actingUserId, UUID clinicId, LocalDateTime from, LocalDateTime to) {
        requireViewAgenda(clinicId, actingUserId);
        if (from == null || to == null || !from.isBefore(to)) {
            throw new IllegalArgumentException("El rango de fechas es inválido.");
        }
        return appointmentRepository.findByClinicIdAndRange(clinicId, from, to);
    }

    @Override
    public List<Appointment> listByQuotation(UUID actingUserId, UUID quotationId, UUID clinicId) {
        requireViewAgenda(clinicId, actingUserId);
        return appointmentRepository.findByQuotationIdAndClinicId(quotationId, clinicId);
    }

    @Override
    public List<Appointment> listByPatient(UUID actingUserId, UUID patientId, UUID clinicId) {
        requireViewAgenda(clinicId, actingUserId);
        return appointmentRepository.findByPatientIdAndClinicId(patientId, clinicId);
    }

    private void requireViewAgenda(UUID clinicId, UUID actingUserId) {
        requirePermission(clinicId, actingUserId, StaffPermission.VIEW_AGENDA,
                "No tienes permiso para consultar la agenda de esta clinica.");
    }

    @Override
    public List<Appointment> listCompletedByClinic(UUID clinicId) {
        return appointmentRepository.findCompletedByClinicId(clinicId);
    }

    @Override
    public Appointment assignPatient(UUID actingUserId, UUID appointmentId, UUID clinicId, UUID patientId) {
        requirePermission(clinicId, actingUserId, StaffPermission.EDIT_APPOINTMENTS,
                "No tienes permiso para editar citas en esta clinica.");
        Appointment appointment = getAppointment(appointmentId, clinicId);
        if (patientId != null) {
            if (!patientValidator.existsByIdAndClinicId(patientId, clinicId)) {
                throw new IllegalArgumentException("El paciente no existe en esta clínica.");
            }
            appointment.setPatientId(patientId);
        } else {
            String currentNotes = appointment.getNotes() != null ? appointment.getNotes() : "";
            if (!currentNotes.contains("[ACKNOWLEDGED]")) {
                appointment.setNotes("[ACKNOWLEDGED] " + currentNotes);
            }
            appointment.setStatus(AppointmentStatus.CONFIRMED);
        }
        appointment.setUpdatedAt(LocalDateTime.now());
        return appointmentRepository.save(appointment);
    }

    @Override
    public List<Appointment> listWithoutPatient(UUID actingUserId, UUID clinicId) {
        requireViewAgenda(clinicId, actingUserId);
        return appointmentRepository.findByClinicIdAndPatientIdIsNull(clinicId);
    }


    @Override
    public List<DoctorSnapshot> listDoctors(UUID actingUserId, UUID clinicId) {
        requireViewAgenda(clinicId, actingUserId);
        return staffValidator.listActiveDoctors(clinicId);
    }

    private void validateTimeRange(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null || !start.isBefore(end)) {
            throw new IllegalArgumentException("La hora de inicio debe ser anterior a la hora de fin.");
        }
        if (start.toLocalDate().equals(end.toLocalDate()) == false) {
            throw new IllegalArgumentException("Una cita no puede cruzar la medianoche entre dos días distintos.");
        }
    }

    private void validateFutureStart(LocalDateTime start, boolean externalImport) {
        if (!externalImport && !start.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("No se puede agendar una cita en un horario que ya pasÃ³.");
        }
    }

    private void validateWithinClinicSchedule(UUID clinicId, LocalDateTime start, LocalDateTime end) {
        ClinicSchedule daySchedule = clinicScheduleService.getEffectiveDay(clinicId, start.getDayOfWeek());
        if (!daySchedule.covers(start.toLocalTime(), end.toLocalTime())) {
            throw new IllegalStateException("El horario solicitado está fuera del horario de atención de la clínica para ese día.");
        }
    }

    private void validateQuotationLink(UUID quotationId, List<UUID> quotationItemIds, UUID patientId, UUID clinicId) {
        AcceptedQuotationSnapshot quotation = quotationValidator.findAcceptedQuotation(quotationId, patientId, clinicId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La cotización indicada no existe, no pertenece a este paciente o no está aceptada."));

        if (quotationItemIds != null && !quotationItemIds.isEmpty()) {
            boolean allItemsExist = quotationItemIds.stream()
                    .allMatch(itemId -> quotation.items().stream().anyMatch(item -> item.itemId().equals(itemId)));
            if (!allItemsExist) {
                throw new IllegalArgumentException("La partida de cotización indicada no pertenece a esta cotización.");
            }
        }
    }

    private List<UUID> normalizeQuotationItemIds(List<UUID> quotationItemIds, UUID legacyQuotationItemId) {
        java.util.LinkedHashSet<UUID> normalized = new java.util.LinkedHashSet<>();
        if (quotationItemIds != null) {
            quotationItemIds.stream().filter(java.util.Objects::nonNull).forEach(normalized::add);
        }
        if (normalized.isEmpty() && legacyQuotationItemId != null) {
            normalized.add(legacyQuotationItemId);
        }
        return List.copyOf(normalized);
    }

    private void validateNoDoctorOverlap(UUID doctorStaffId, UUID clinicId, LocalDateTime start, LocalDateTime end, UUID excludeAppointmentId) {
        List<Appointment> doctorAppointments = appointmentRepository.findActiveByDoctorAndRange(doctorStaffId, clinicId, start, end);
        boolean hasOverlap = doctorAppointments.stream()
                .filter(a -> !a.getId().equals(excludeAppointmentId))
                .anyMatch(a -> a.overlapsWith(start, end));
        if (hasOverlap) {
            throw new IllegalStateException("El doctor ya tiene otra cita agendada en ese horario.");
        }
    }

    /**
     * Un cupo apartado por una solicitud de cita en linea queda bloqueado tambien para el personal
     * mientras el medico decide (decision de la entrega 3). Vencido o liberado deja de contar.
     */
    private void validateNoActiveHold(UUID doctorStaffId, UUID clinicId, LocalDateTime start, LocalDateTime end) {
        if (slotHoldRepository == null) {
            return;
        }
        if (!slotHoldRepository.findActiveByDoctorAndRange(doctorStaffId, clinicId, start, end, LocalDateTime.now()).isEmpty()) {
            throw new IllegalStateException(
                    "Ese horario está apartado por una solicitud de cita en espera de respuesta del médico.");
        }
    }

    private void validateNoRoomOverlap(UUID roomId, UUID clinicId, LocalDateTime start, LocalDateTime end, UUID excludeAppointmentId) {
        if (roomId == null) {
            return;
        }
        boolean hasOverlap = appointmentRepository.existsOverlappingAppointmentByRoom(roomId, clinicId, start, end, excludeAppointmentId);
        if (hasOverlap) {
            throw new IllegalStateException("El consultorio o sillón seleccionado ya está ocupado en ese horario por otra cita.");
        }
    }

    private void validateActiveRoom(UUID roomId, UUID clinicId) {
        if (roomValidator == null) {
            return;
        }
        roomValidator.findActiveRoom(roomId, clinicId)
                .orElseThrow(() -> new IllegalArgumentException("El consultorio no existe o está inactivo en esta clínica."));
    }

    private void validateNoRoomBlockOverlap(UUID roomId, UUID clinicId, LocalDateTime start, LocalDateTime end) {
        if (roomBlockRepository != null
                && roomBlockRepository.existsOverlappingActiveByRoom(roomId, clinicId, start, end)) {
            throw new IllegalStateException("El consultorio está bloqueado por mantenimiento o uso interno en ese horario.");
        }
    }
}
