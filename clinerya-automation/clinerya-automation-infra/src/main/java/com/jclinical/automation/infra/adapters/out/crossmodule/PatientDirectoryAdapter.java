package com.jclinical.automation.infra.adapters.out.crossmodule;

import com.jclinical.automation.domain.ports.out.PatientDirectoryPort;
import com.jclinical.patients.domain.model.Patient;
import com.jclinical.patients.domain.ports.in.GetPatientUseCase;

import java.util.List;
import java.util.UUID;

/**
 * Busca pacientes de la clinica por celular. WhatsApp entrega el numero en formato internacional
 * (5215512345678) y la clinica lo captura como lo dicta el paciente (55 1234-5678), asi que se
 * comparan los ultimos 10 digitos, el numero nacional en Mexico.
 *
 * <p>Filtra en memoria sobre el padron de la clinica: suficiente para el volumen de un consultorio.
 * Si una clinica crece a decenas de miles de pacientes conviene una consulta por sufijo en pacientes.
 */
public class PatientDirectoryAdapter implements PatientDirectoryPort {

    static final int NATIONAL_DIGITS = 10;

    private final GetPatientUseCase getPatientUseCase;

    public PatientDirectoryAdapter(GetPatientUseCase getPatientUseCase) {
        this.getPatientUseCase = getPatientUseCase;
    }

    @Override
    public List<PatientContact> findByPhone(UUID clinicId, String phone) {
        String wanted = nationalNumber(phone);
        if (wanted == null) {
            return List.of();
        }
        return getPatientUseCase.getPatientsByClinic(clinicId).stream()
                .filter(patient -> wanted.equals(nationalNumber(patient.getPhone())))
                .map(patient -> new PatientContact(patient.getId(), displayName(patient)))
                .toList();
    }

    static String nationalNumber(String phone) {
        if (phone == null) {
            return null;
        }
        String digits = phone.replaceAll("[^0-9]", "");
        return digits.length() < NATIONAL_DIGITS ? null : digits.substring(digits.length() - NATIONAL_DIGITS);
    }

    private static String displayName(Patient patient) {
        return (patient.getFirstName() + " " + patient.getLastNamePaterno()).trim();
    }
}
