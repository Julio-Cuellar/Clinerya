package com.jclinical.automation.domain.model;

import java.util.List;
import java.util.UUID;

/**
 * Aviso al medico por WhatsApp. {@code text} sale dentro de la ventana de 24 h; fuera de ella se usa
 * la plantilla aprobada de la clinica con {@code templateParameters}.
 */
public record DoctorNotice(UUID clinicId, String phone, String text, List<String> templateParameters) {

    public DoctorNotice {
        templateParameters = templateParameters == null ? List.of() : List.copyOf(templateParameters);
    }
}
