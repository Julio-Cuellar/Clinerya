package com.jclinical.automation.domain.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Ficha breve de quien escribe por WhatsApp: el numero, su nombre de perfil, desde cuando escribe,
 * los pacientes registrados con ese celular y senales que orientan si parece spam (no bloquean nada).
 */
public record ChatContact(String phone, String profileName, LocalDateTime firstMessageAt, int messageCount,
                          List<ContactPatient> patients, List<Signal> signals) {

    public ChatContact {
        patients = patients == null ? List.of() : List.copyOf(patients);
        signals = signals == null ? List.of() : List.copyOf(signals);
    }

    /** Solo identificacion y citas; nada del expediente clinico. {@code age} null si no hay fecha de nacimiento. */
    public record ContactPatient(UUID patientId, String fullName, Integer age, LocalDateTime registeredAt,
                                 boolean whatsappConsent, LocalDateTime nextStart, String nextDoctorName,
                                 LocalDateTime lastAttendedStart, String lastAttendedDoctorName, int attended,
                                 int cancelled, int noShows) {}

    public enum Signal {
        /** El celular no es de Mexico. */
        FOREIGN_NUMBER,
        /** Su primer mensaje trae un enlace. */
        FIRST_MESSAGE_HAS_LINK,
        /** Ni el numero ni sus pacientes han tenido cita en la clinica. */
        NEVER_HAD_APPOINTMENT
    }
}
