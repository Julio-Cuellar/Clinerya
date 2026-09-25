package com.jclinical.automation.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Celular donde el medico recibe avisos de la clinica por WhatsApp (D8). Solo recibe: responde en
 * Clinerya. {@code consentAt} es cuando acepto recibirlos; sin consentimiento no se activa.
 */
public record DoctorChannel(
        UUID clinicId,
        UUID staffId,
        String phone,
        boolean active,
        LocalDateTime consentAt,
        UUID updatedBy,
        LocalDateTime updatedAt
) {}
