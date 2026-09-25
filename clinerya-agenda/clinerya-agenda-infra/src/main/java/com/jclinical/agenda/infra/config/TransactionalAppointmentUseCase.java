package com.jclinical.agenda.infra.config;

import com.jclinical.agenda.domain.model.Appointment;
import com.jclinical.agenda.domain.model.AppointmentStatus;
import com.jclinical.agenda.domain.ports.in.ManageAppointmentsUseCase;
import com.jclinical.agenda.domain.ports.out.StaffValidatorPort.DoctorSnapshot;
import com.jclinical.agenda.domain.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Una transaccion por caso de uso: la cita y el evento que deja en el outbox se confirman o se
 * revierten juntos. Es @Primary para que todo consumidor de ManageAppointmentsUseCase (controlador,
 * caja, integraciones, sembradores) reciba la envoltura y no el servicio desnudo.
 */
@Service
@Primary
@RequiredArgsConstructor
public class TransactionalAppointmentUseCase implements ManageAppointmentsUseCase {

    private final AppointmentService appointmentService;

    @Override
    @Transactional
    public Appointment createAppointment(UUID actingUserId, UUID clinicId, CreateAppointmentCommand command) {
        return appointmentService.createAppointment(actingUserId, clinicId, command);
    }

    @Override
    @Transactional
    public List<Appointment> createAppointmentSeries(UUID actingUserId, UUID clinicId, CreateAppointmentSeriesCommand command) {
        return appointmentService.createAppointmentSeries(actingUserId, clinicId, command);
    }

    @Override
    @Transactional
    public Appointment rescheduleAppointment(UUID actingUserId, UUID appointmentId, UUID clinicId,
                                             LocalDateTime newStart, LocalDateTime newEnd, boolean externalImport) {
        return appointmentService.rescheduleAppointment(actingUserId, appointmentId, clinicId, newStart, newEnd, externalImport);
    }

    @Override
    @Transactional
    public Appointment transitionStatus(UUID actingUserId, UUID appointmentId, UUID clinicId, AppointmentStatus targetStatus,
                                        String cancellationReason, UUID cancelledByUserId) {
        return appointmentService.transitionStatus(actingUserId, appointmentId, clinicId, targetStatus,
                cancellationReason, cancelledByUserId);
    }

    @Override
    @Transactional
    public Appointment attachExternalCalendarEvent(UUID appointmentId, UUID clinicId, String externalCalendarEventId) {
        return appointmentService.attachExternalCalendarEvent(appointmentId, clinicId, externalCalendarEventId);
    }

    @Override
    @Transactional
    public void deleteAppointment(UUID actingUserId, UUID appointmentId, UUID clinicId) {
        appointmentService.deleteAppointment(actingUserId, appointmentId, clinicId);
    }

    @Override
    @Transactional(readOnly = true)
    public Appointment getAppointment(UUID appointmentId, UUID clinicId) {
        return appointmentService.getAppointment(appointmentId, clinicId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Appointment> listByClinicRange(UUID actingUserId, UUID clinicId, LocalDateTime from, LocalDateTime to) {
        return appointmentService.listByClinicRange(actingUserId, clinicId, from, to);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Appointment> listByQuotation(UUID actingUserId, UUID quotationId, UUID clinicId) {
        return appointmentService.listByQuotation(actingUserId, quotationId, clinicId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Appointment> listByPatient(UUID actingUserId, UUID patientId, UUID clinicId) {
        return appointmentService.listByPatient(actingUserId, patientId, clinicId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Appointment> listCompletedByClinic(UUID clinicId) {
        return appointmentService.listCompletedByClinic(clinicId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DoctorSnapshot> listDoctors(UUID actingUserId, UUID clinicId) {
        return appointmentService.listDoctors(actingUserId, clinicId);
    }

    @Override
    @Transactional
    public Appointment assignPatient(UUID actingUserId, UUID appointmentId, UUID clinicId, UUID patientId) {
        return appointmentService.assignPatient(actingUserId, appointmentId, clinicId, patientId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Appointment> listWithoutPatient(UUID actingUserId, UUID clinicId) {
        return appointmentService.listWithoutPatient(actingUserId, clinicId);
    }
}
