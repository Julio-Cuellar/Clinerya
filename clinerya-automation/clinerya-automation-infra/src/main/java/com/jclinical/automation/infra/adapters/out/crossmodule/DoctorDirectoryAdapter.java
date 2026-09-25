package com.jclinical.automation.infra.adapters.out.crossmodule;

import com.jclinical.agenda.domain.model.Appointment;
import com.jclinical.agenda.domain.model.AppointmentStatus;
import com.jclinical.agenda.domain.ports.in.ManageAppointmentsUseCase;
import com.jclinical.automation.domain.ports.out.DoctorDirectoryPort;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Medicos que atienden en la clinica, leidos de la agenda por su ruta interna (sin usuario: la
 * conversacion la inicia el paciente, no un miembro del personal).
 */
public class DoctorDirectoryAdapter implements DoctorDirectoryPort {

    private final ManageAppointmentsUseCase appointments;

    public DoctorDirectoryAdapter(ManageAppointmentsUseCase appointments) {
        this.appointments = appointments;
    }

    @Override
    public List<DoctorContact> listDoctors(UUID clinicId) {
        return appointments.listDoctors(null, clinicId).stream()
                .map(doctor -> new DoctorContact(doctor.staffId(), doctor.fullName()))
                .toList();
    }

    /** Medico de la ultima cita completada, siempre que siga atendiendo en la clinica. */
    @Override
    public Optional<DoctorContact> lastDoctorOf(UUID clinicId, UUID patientId) {
        Optional<UUID> lastDoctorId = appointments.listByPatient(null, patientId, clinicId).stream()
                .filter(appointment -> appointment.getStatus() == AppointmentStatus.COMPLETED)
                .max(Comparator.comparing(Appointment::getScheduledStart))
                .map(Appointment::getDoctorStaffId);
        return lastDoctorId.flatMap(doctorId -> listDoctors(clinicId).stream()
                .filter(doctor -> doctor.staffId().equals(doctorId))
                .findFirst());
    }
}
