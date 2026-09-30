package com.jclinical.automation.domain.model;

import java.util.List;
import java.util.UUID;

/**
 * Mensaje que la automatizacion le debe enviar al paciente por iniciativa propia.
 * {@code authorUserId}: quien del personal lo escribio; null si lo envia el asistente.
 * {@code templateName}: plantilla propia de este mensaje si la ventana de 24 h esta cerrada (null: la
 * general de pacientes); {@code templateParameters}: sus valores (vacio: el texto completo).
 */
public record PatientNotification(UUID clinicId, String phone, OutboundReply reply, UUID authorUserId,
                                  String templateName, List<String> templateParameters) {

    public PatientNotification {
        templateParameters = templateParameters == null ? List.of() : List.copyOf(templateParameters);
    }

    public PatientNotification(UUID clinicId, String phone, OutboundReply reply, UUID authorUserId) {
        this(clinicId, phone, reply, authorUserId, null, List.of());
    }

    public PatientNotification(UUID clinicId, String phone, OutboundReply reply) {
        this(clinicId, phone, reply, null);
    }
}
