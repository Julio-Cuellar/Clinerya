package com.jclinical.patients.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Decision del paciente sobre recibir mensajes por iniciativa de la clinica (recordatorios,
 * seguimiento). Se guarda tambien el "no" explicito: {@code recordedAt == null} significa que nunca
 * se le pregunto.
 */
public record ContactConsent(
        boolean granted,
        String textVersion,
        ConsentSource source,
        UUID recordedByUserId,
        LocalDateTime recordedAt
) {

    public static ContactConsent notRecorded() {
        return new ContactConsent(false, null, null, null, null);
    }

    public boolean allowsProactiveContact() {
        return granted;
    }
}
