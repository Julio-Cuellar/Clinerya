package com.jclinical.automation.domain.ports.out;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Pacientes registrados con un celular, con lo justo para reconocerlos en Chats. Se arma con las rutas
 * publicas de pacientes y agenda; nunca se leen sus tablas.
 */
@FunctionalInterface
public interface PatientSnapshotPort {

    List<PatientSnapshot> findByPhone(UUID clinicId, String phone);

    record PatientSnapshot(UUID patientId, String fullName, LocalDate dateOfBirth, LocalDateTime registeredAt,
                           boolean whatsappConsent, LocalDateTime nextStart, UUID nextDoctorStaffId,
                           LocalDateTime lastAttendedStart, UUID lastAttendedDoctorStaffId, int attended, int cancelled,
                           int noShows) {}
}
