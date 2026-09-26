package com.jclinical.automation.domain.ports.out;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Alta de un paciente nuevo desde el chat, por la API publica del modulo de pacientes. El
 * consentimiento de contacto queda registrado como dado por el propio paciente en WhatsApp.
 */
public interface PatientRegistrationPort {

    /** Texto vigente de la autorizacion de contacto y su version (la misma que se lee en el alta). */
    ConsentText consentText();

    /** @return id del paciente creado */
    UUID register(NewPatient patient);

    record ConsentText(String version, String text) {}

    record NewPatient(UUID clinicId, String firstName, String lastNamePaterno, String lastNameMaterno,
                      LocalDate dateOfBirth, String phone, String email, String consentVersion) {}
}
