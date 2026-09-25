package com.jclinical.agenda.infra.config;

import com.jclinical.agenda.domain.model.Appointment;
import com.jclinical.agenda.domain.model.AppointmentStatus;
import com.jclinical.agenda.domain.ports.in.ManageAppointmentsUseCase;
import com.jclinical.agenda.domain.ports.out.StaffValidatorPort.DoctorSnapshot;
import com.jclinical.agenda.domain.service.AppointmentService;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class TransactionalAppointmentUseCase implements ManageAppointmentsUseCase {

    private final AppointmentService appointmentService;

    @Override
    public Appointment createAppointment(UUID actingUserId, UUID clinicId, CreateAppointmentCommand command) {
        return appointmentService.createAppointment(actingUserId, clinicId, command);
    }

    @Override
    public List<Appointment> createAppointmentSeries(UUID actingUserId, UUID clinicId, CreateAppointmentSeriesCommand command) {
        return appointmentService.createAppointmentSeries(actingUserId, clinicId, command);
    }

    @Override
    public Appointment rescheduleAppointment(UUID actingUserId, UUID appointmentId, UUID clinicId,
                                             LocalDateTime newStart, LocalDateTime newEnd, boolean externalImport) {
        return appointmentService.rescheduleAppointment(actingUserId, appointmentId, clinicId, newStart, newEnd, externalImport);
    }

    @Override
    public Appointment transitionStatus(UUID actingUserId, UUID appointmentId, UUID clinicId, AppointmentStatus targetStatus,
                                        String cancellationReason, UUID cancelledByUserId) {
        return appointmentService.transitionStatus(actingUserId, appointmentId, clinicId, targetStatus,
                cancellationReason, cancelledByUserId);
    }

    @Override
    public Appointment attachExternalCalendarEvent(UUID appointmentId, UUID clinicId, String externalCalendarEventId) {
        return appointmentService.attachExternalCalendarEvent(appointmentId, clinicId, externalCalendarEventId);
    }

    @Override
    public void deleteAppointment(UUID actingUserId, UUID appointmentId, UUID clinicId) {
        appointmentService.deleteAppointment(actingUserId, appointmentId, clinicId);
    }

    @Override
    public Appointment getAppointment(UUID appointmentId, UUID clinicId) {
        return appointmentService.getAppointment(appointmentId, clinicId);
    }

    @Override
    public List<Appointment> listByClinicRange(UUID actingUserId, UUID clinicId, LocalDateTime from, LocalDateTime to) {
        return appointmentService.listByClinicRange(actingUserId, clinicId, from, to);
    }

    @Override
    public List<Appointment> listByQuotation(UUID actingUserId, UUID quotationId, UUID clinicId) {
        return appointmentService.listByQuotation(actingUserId, quotationId, clinicId);
    }

    @Override
    public List<Appointment> listByPatient(UUID actingUserId, UUID patientId, UUID clinicId) {
        return appointmentService.listByPatient(actingUserId, patientId, clinicId);
    }

    @Override
    public List<Appointment> listCompletedByClinic(UUID clinicId) {
        return appointmentService.listCompletedByClinic(clinicId);
    }

    @Override
    public List<DoctorSnapshot> listDoctors(UUID actingUserId, UUID clinicId) {
        return appointmentService.listDoctors(actingUserId, clinicId);
    }

    @Override
    public Appointment assignPatient(UUID actingUserId, UUID appointmentId, UUID clinicId, UUID patientId) {
        return appointmentService.assignPatient(actingUserId, appointmentId, clinicId, patientId);
    }

    @Override
    public List<Appointment> listWithoutPatient(UUID actingUserId, UUID clinicId) {
        return appointmentService.listWithoutPatient(actingUserId, clinicId);
    }
}
