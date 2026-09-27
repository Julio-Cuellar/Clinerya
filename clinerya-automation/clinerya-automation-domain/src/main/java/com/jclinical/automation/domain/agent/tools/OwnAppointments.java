package com.jclinical.automation.domain.agent.tools;

import com.jclinical.automation.domain.agent.ToolContext;
import com.jclinical.automation.domain.ports.out.DoctorDirectoryPort;
import com.jclinical.automation.domain.ports.out.DoctorDirectoryPort.DoctorContact;
import com.jclinical.automation.domain.ports.out.PatientAppointmentsPort;
import com.jclinical.automation.domain.ports.out.PatientAppointmentsPort.UpcomingVisit;
import com.jclinical.automation.domain.ports.out.PatientDirectoryPort.PatientContact;

import java.util.Optional;

/** Una cita "cita:id" solo cuenta si es proxima y de alguno de los pacientes de ese celular (lo dice la agenda). */
final class OwnAppointments {

    static final String PREFIX = "cita:";
    private static final int LOOKUP_LIMIT = 10;

    record Found(PatientContact patient, UpcomingVisit visit, String doctorName) {}

    private OwnAppointments() {
    }

    static Optional<Found> find(PatientAppointmentsPort appointments, DoctorDirectoryPort doctors, ToolContext context, String requested) {
        for (PatientContact patient : context.patients()) {
            for (UpcomingVisit visit : appointments.upcoming(context.clinicId(), patient.patientId(), LOOKUP_LIMIT)) {
                if ((PREFIX + visit.appointmentId()).equals(requested)) {
                    String doctor = doctors.listDoctors(context.clinicId()).stream()
                            .filter(d -> d.staffId().equals(visit.doctorStaffId()))
                            .map(DoctorContact::displayName).findFirst().orElse("tu médico");
                    return Optional.of(new Found(patient, visit, doctor));
                }
            }
        }
        return Optional.empty();
    }
}
