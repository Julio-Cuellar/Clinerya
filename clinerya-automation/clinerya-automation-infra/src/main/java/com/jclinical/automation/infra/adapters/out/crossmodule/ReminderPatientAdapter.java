package com.jclinical.automation.infra.adapters.out.crossmodule;

import com.jclinical.automation.domain.ports.out.ReminderPatientPort;
import com.jclinical.patients.domain.model.Patient;
import com.jclinical.patients.domain.ports.in.GetPatientUseCase;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A quien se le recuerda la cita, por la ruta publica de pacientes. El celular se lleva al formato con el
 * que WhatsApp reporta los moviles de Mexico (52 + 1 + 10 digitos), igual que el directorio de pacientes.
 */
public class ReminderPatientAdapter implements ReminderPatientPort {

    private static final String MEXICO_MOBILE = "521";

    private final GetPatientUseCase patients;

    public ReminderPatientAdapter(GetPatientUseCase patients) {
        this.patients = patients;
    }

    @Override
    public Optional<ReminderRecipient> find(UUID clinicId, UUID patientId) {
        if (patientId == null) {
            return Optional.empty();
        }
        return patients.getPatientById(patientId)
                .filter(patient -> clinicId.equals(patient.getClinicId()))
                .map(patient -> new ReminderRecipient(fullName(patient), whatsAppNumber(patient.getPhone()), consent(patient)));
    }

    private static String whatsAppNumber(String phone) {
        String national = PatientDirectoryAdapter.nationalNumber(phone);
        return national == null ? null : MEXICO_MOBILE + national;
    }

    private static boolean consent(Patient patient) {
        return patient.getContactConsent() != null && patient.getContactConsent().allowsProactiveContact();
    }

    private static String fullName(Patient patient) {
        return Stream.of(patient.getFirstName(), patient.getLastNamePaterno(), patient.getLastNameMaterno())
                .filter(Objects::nonNull)
                .map(String::strip)
                .filter(part -> !part.isEmpty())
                .collect(Collectors.joining(" "));
    }
}
