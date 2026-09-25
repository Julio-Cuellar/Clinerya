package com.jclinical.automation.domain.ports.out;

import java.util.List;
import java.util.UUID;

public interface PatientDirectoryPort {
    /** Pacientes de la clinica registrados con ese celular (puede haber varios: familia). */
    List<PatientContact> findByPhone(UUID clinicId, String phone);

    record PatientContact(UUID patientId, String displayName) {}
}
