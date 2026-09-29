package com.jclinical.automation.domain.model;

import java.util.UUID;

/**
 * Mensaje que la automatizacion le debe enviar al paciente por iniciativa propia.
 * {@code authorUserId}: quien del personal lo escribio; null si lo envia el asistente.
 */
public record PatientNotification(UUID clinicId, String phone, OutboundReply reply, UUID authorUserId) {

    public PatientNotification(UUID clinicId, String phone, OutboundReply reply) {
        this(clinicId, phone, reply, null);
    }
}
