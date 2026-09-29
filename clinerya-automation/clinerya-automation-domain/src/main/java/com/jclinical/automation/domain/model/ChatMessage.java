package com.jclinical.automation.domain.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Un mensaje del hilo de WhatsApp entre un celular y la clinica. Se guarda cifrado; las opciones que
 * se le ofrecieron al paciente se guardan como etiquetas para mostrar el chat tal como lo vio.
 * {@code authorUserId}: quien del personal lo escribio (solo en {@link Direction#STAFF}).
 */
public record ChatMessage(
        UUID id,
        UUID clinicId,
        String phone,
        Direction direction,
        String text,
        List<String> optionLabels,
        LocalDateTime at,
        UUID authorUserId
) {
    public enum Direction {
        INBOUND,
        /** Lo envio el asistente. */
        OUTBOUND,
        /** Lo escribio alguien de la clinica desde Chats (atencion humana). */
        STAFF
    }

    public ChatMessage {
        optionLabels = optionLabels == null ? List.of() : List.copyOf(optionLabels);
    }

    public ChatMessage(UUID id, UUID clinicId, String phone, Direction direction, String text, List<String> optionLabels,
                       LocalDateTime at) {
        this(id, clinicId, phone, direction, text, optionLabels, at, null);
    }
}
