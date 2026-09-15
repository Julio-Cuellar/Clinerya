package com.jclinical.agenda.domain.ports.in;

import com.jclinical.agenda.domain.model.Appointment;
import com.jclinical.agenda.domain.model.AppointmentStatus;
import com.jclinical.agenda.domain.ports.out.StaffValidatorPort.DoctorSnapshot;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ManageAppointmentsUseCase {

    /** Ruta interna (importacion de calendario, seeders): sin control de permiso. */
    default Appointment createAppointment(UUID clinicId, CreateAppointmentCommand command) {
        return createAppointment(null, clinicId, command);
    }

    Appointment createAppointment(UUID actingUserId, UUID clinicId, CreateAppointmentCommand command);

    List<Appointment> createAppointmentSeries(UUID actingUserId, UUID clinicId, CreateAppointmentSeriesCommand command);

    default Appointment rescheduleAppointment(UUID appointmentId, UUID clinicId, LocalDateTime newStart, LocalDateTime newEnd) {
        return rescheduleAppointment(null, appointmentId, clinicId, newStart, newEnd, false);
    }

    /** Ruta interna (sincronizacion de calendario externo): sin control de permiso. */
    default Appointment rescheduleAppointment(UUID appointmentId, UUID clinicId, LocalDateTime newStart, LocalDateTime newEnd, boolean externalImport) {
        return rescheduleAppointment(null, appointmentId, clinicId, newStart, newEnd, externalImport);
    }

    Appointment rescheduleAppointment(UUID actingUserId, UUID appointmentId, UUID clinicId, LocalDateTime newStart, LocalDateTime newEnd, boolean externalImport);

    default Appointment transitionStatus(UUID appointmentId, UUID clinicId, AppointmentStatus targetStatus) {
        return transitionStatus(null, appointmentId, clinicId, targetStatus, null, null);
    }

    /** Ruta interna (sincronizacion de calendario externo, seeders): sin control de permiso. */
    default Appointment transitionStatus(UUID appointmentId, UUID clinicId, AppointmentStatus targetStatus, String cancellationReason, UUID cancelledByUserId) {
        return transitionStatus(null, appointmentId, clinicId, targetStatus, cancellationReason, cancelledByUserId);
    }

    Appointment transitionStatus(UUID actingUserId, UUID appointmentId, UUID clinicId, AppointmentStatus targetStatus, String cancellationReason, UUID cancelledByUserId);

    /** Sin control de permiso: paso interno del flujo de vinculacion de eventos externos. */
    Appointment attachExternalCalendarEvent(UUID appointmentId, UUID clinicId, String externalCalendarEventId);

    void deleteAppointment(UUID actingUserId, UUID appointmentId, UUID clinicId);

    /** Sin control de permiso: lo consumen orquestadores internos de integraciones. */
    Appointment getAppointment(UUID appointmentId, UUID clinicId);

    List<Appointment> listByClinicRange(UUID actingUserId, UUID clinicId, LocalDateTime from, LocalDateTime to);

    List<Appointment> listByQuotation(UUID actingUserId, UUID quotationId, UUID clinicId);

    List<Appointment> listByPatient(UUID actingUserId, UUID patientId, UUID clinicId);

    /** Sin control de permiso: lo consume el modulo de caja (cobros pendientes). */
    List<Appointment> listCompletedByClinic(UUID clinicId);

    List<DoctorSnapshot> listDoctors(UUID actingUserId, UUID clinicId);

    Appointment assignPatient(UUID actingUserId, UUID appointmentId, UUID clinicId, UUID patientId);

    List<Appointment> listWithoutPatient(UUID actingUserId, UUID clinicId);

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
