package com.jclinical.agenda.domain.ports.in;

import com.jclinical.agenda.domain.model.Appointment;
import com.jclinical.agenda.domain.model.AppointmentStatus;
import com.jclinical.agenda.domain.ports.out.StaffValidatorPort.DoctorSnapshot;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ManageAppointmentsUseCase {

    Appointment createAppointment(UUID clinicId, UUID actingUserId, CreateAppointmentCommand command);

    /**
     * Usado por la sincronizacion con Google Calendar (importacion de eventos externos):
     * no hay usuario en la peticion. No exponer desde un controlador.
     */
    Appointment createAppointmentForSystem(UUID clinicId, CreateAppointmentCommand command);

    List<Appointment> createAppointmentSeries(UUID clinicId, UUID actingUserId, CreateAppointmentSeriesCommand command);

    Appointment rescheduleAppointment(UUID appointmentId, UUID clinicId, UUID actingUserId, LocalDateTime newStart, LocalDateTime newEnd);

    /**
     * Usado por la sincronizacion con Google Calendar: no hay usuario en la peticion.
     * No exponer desde un controlador.
     */
    Appointment rescheduleAppointmentForSystem(UUID appointmentId, UUID clinicId, LocalDateTime newStart, LocalDateTime newEnd, boolean externalImport);

    Appointment transitionStatus(UUID appointmentId, UUID clinicId, UUID actingUserId, AppointmentStatus targetStatus, String cancellationReason);

    /**
     * Usado por la sincronizacion con Google Calendar (cancelacion detectada externamente):
     * no hay usuario en la peticion. No exponer desde un controlador.
     */
    Appointment transitionStatusForSystem(UUID appointmentId, UUID clinicId, AppointmentStatus targetStatus, String cancellationReason, UUID cancelledByUserId);

    /**
     * Solo lo usa la integracion con Google Calendar para enlazar el evento externo tras
     * crear o sincronizar una cita: no hay usuario en la peticion. No exponer desde un
     * controlador.
     */
    Appointment attachExternalCalendarEvent(UUID appointmentId, UUID clinicId, String externalCalendarEventId);

    void deleteAppointment(UUID appointmentId, UUID clinicId, UUID actingUserId);

    Appointment getAppointment(UUID appointmentId, UUID clinicId, UUID actingUserId);

    /**
     * Lectura interna para la sincronizacion con Google Calendar: no hay usuario en la
     * peticion. No exponer desde un controlador.
     */
    Appointment getAppointmentForSystem(UUID appointmentId, UUID clinicId);

    List<Appointment> listByClinicRange(UUID clinicId, UUID actingUserId, LocalDateTime from, LocalDateTime to);

    List<Appointment> listByQuotation(UUID quotationId, UUID clinicId, UUID actingUserId);

    List<Appointment> listByPatient(UUID patientId, UUID clinicId, UUID actingUserId);

    /**
     * Lectura interna para caja (cobros pendientes de citas completadas): no hay usuario
     * en la peticion. No exponer desde un controlador.
     */
    List<Appointment> listCompletedByClinic(UUID clinicId);

    List<DoctorSnapshot> listDoctors(UUID clinicId, UUID actingUserId);

    Appointment assignPatient(UUID appointmentId, UUID clinicId, UUID actingUserId, UUID patientId);

    List<Appointment> listWithoutPatient(UUID clinicId, UUID actingUserId);

    record CreateAppointmentCommand(
            UUID patientId,
            UUID doctorStaffId,
            UUID roomId,
            UUID quotationId,
            UUID quotationItemId,
            List<UUID> quotationItemIds,
            LocalDateTime scheduledStart,
            LocalDateTime scheduledEnd,
            String reason,
            String notes,
            boolean externalImport
    ) {
        public CreateAppointmentCommand(
                UUID patientId,
                UUID doctorStaffId,
                UUID quotationId,
                UUID quotationItemId,
                LocalDateTime scheduledStart,
                LocalDateTime scheduledEnd,
                String reason,
                String notes) {
            this(patientId, doctorStaffId, null, quotationId, quotationItemId,
                    quotationItemId == null ? List.of() : List.of(quotationItemId),
                    scheduledStart, scheduledEnd, reason, notes, false);
        }

        public CreateAppointmentCommand(
                UUID patientId,
                UUID doctorStaffId,
                UUID roomId,
                UUID quotationId,
                UUID quotationItemId,
                List<UUID> quotationItemIds,
                LocalDateTime scheduledStart,
                LocalDateTime scheduledEnd,
                String reason,
                String notes) {
            this(patientId, doctorStaffId, roomId, quotationId, quotationItemId,
                    quotationItemIds, scheduledStart, scheduledEnd, reason, notes, false);
        }

        public CreateAppointmentCommand(
                UUID patientId,
                UUID doctorStaffId,
                UUID roomId,
                UUID quotationId,
                UUID quotationItemId,
                LocalDateTime scheduledStart,
                LocalDateTime scheduledEnd,
                String reason,
                String notes) {
            this(patientId, doctorStaffId, roomId, quotationId, quotationItemId,
                    quotationItemId == null ? List.of() : List.of(quotationItemId),
                    scheduledStart, scheduledEnd, reason, notes, false);
        }
    }

    enum RecurrenceFrequency {
        DAILY,
        WEEKLY,
        BIWEEKLY,
        MONTHLY
    }

    record CreateAppointmentSeriesCommand(
            UUID patientId,
            UUID doctorStaffId,
            UUID roomId,
            UUID quotationId,
            UUID quotationItemId,
            List<UUID> quotationItemIds,
            LocalDateTime firstScheduledStart,
            LocalDateTime firstScheduledEnd,
            RecurrenceFrequency frequency,
            int repeatCount,
            String reason,
            String notes
    ) {}
}
