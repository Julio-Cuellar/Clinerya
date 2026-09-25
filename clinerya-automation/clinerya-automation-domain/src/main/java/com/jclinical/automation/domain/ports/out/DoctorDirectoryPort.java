package com.jclinical.automation.domain.ports.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DoctorDirectoryPort {
    /** Personal que atiende pacientes en la clinica. */
    List<DoctorContact> listDoctors(UUID clinicId);

    /** Medico de la ultima cita completada del paciente. */
    Optional<DoctorContact> lastDoctorOf(UUID clinicId, UUID patientId);

    record DoctorContact(UUID staffId, String displayName) {}
}
