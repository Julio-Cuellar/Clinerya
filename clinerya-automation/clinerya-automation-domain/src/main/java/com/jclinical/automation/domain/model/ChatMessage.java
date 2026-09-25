package com.jclinical.automation.domain.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Un mensaje del hilo de WhatsApp entre un celular y la clinica. Se guarda cifrado; las opciones que
 * se le ofrecieron al paciente se guardan como etiquetas para mostrar el chat tal como lo vio.
 */
public record ChatMessage(
        UUID id,
        UUID clinicId,
        String phone,
        Direction direction,
        String text,
        List<String> optionLabels,
        LocalDateTime at
) {

    public enum Direction {
        INBOUND,
        OUTBOUND
    }

    public ChatMessage {
        optionLabels = optionLabels == null ? List.of() : List.copyOf(optionLabels);
    }
}
