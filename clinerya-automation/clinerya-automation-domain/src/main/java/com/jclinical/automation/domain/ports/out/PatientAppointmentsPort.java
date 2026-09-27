package com.jclinical.automation.domain.ports.out;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Proximas citas vigentes de un paciente, leidas de la agenda (ruta interna). */
@FunctionalInterface
public interface PatientAppointmentsPort {

    List<UpcomingVisit> upcoming(UUID clinicId, UUID patientId, int limit);

    record UpcomingVisit(UUID appointmentId, UUID doctorStaffId, LocalDateTime start, LocalDateTime end, boolean confirmed) {}
}
