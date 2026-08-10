package com.jclinical.agenda.domain.ports.in;

import com.jclinical.agenda.domain.model.Appointment;
import com.jclinical.agenda.domain.model.AppointmentStatus;
import com.jclinical.agenda.domain.ports.out.StaffValidatorPort.DoctorSnapshot;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ManageAppointmentsUseCase {

    Appointment createAppointment(UUID clinicId, CreateAppointmentCommand command);

    List<Appointment> createAppointmentSeries(UUID clinicId, CreateAppointmentSeriesCommand command);

    default Appointment rescheduleAppointment(UUID appointmentId, UUID clinicId, LocalDateTime newStart, LocalDateTime newEnd) {
        return rescheduleAppointment(appointmentId, clinicId, newStart, newEnd, false);
    }

    Appointment rescheduleAppointment(UUID appointmentId, UUID clinicId, LocalDateTime newStart, LocalDateTime newEnd, boolean externalImport);

    default Appointment transitionStatus(UUID appointmentId, UUID clinicId, AppointmentStatus targetStatus) {
        return transitionStatus(appointmentId, clinicId, targetStatus, null, null);
    }

    Appointment transitionStatus(UUID appointmentId, UUID clinicId, AppointmentStatus targetStatus, String cancellationReason, UUID cancelledByUserId);

    Appointment attachExternalCalendarEvent(UUID appointmentId, UUID clinicId, String externalCalendarEventId);

    void deleteAppointment(UUID appointmentId, UUID clinicId);

    Appointment getAppointment(UUID appointmentId, UUID clinicId);

    List<Appointment> listByClinicRange(UUID clinicId, LocalDateTime from, LocalDateTime to);

    List<Appointment> listByQuotation(UUID quotationId, UUID clinicId);

    List<Appointment> listCompletedByClinic(UUID clinicId);

    List<DoctorSnapshot> listDoctors(UUID clinicId);

    Appointment assignPatient(UUID appointmentId, UUID clinicId, UUID patientId);

    List<Appointment> listWithoutPatient(UUID clinicId);

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
