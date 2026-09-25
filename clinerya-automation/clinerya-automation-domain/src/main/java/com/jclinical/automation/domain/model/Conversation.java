package com.jclinical.automation.domain.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Conversacion de un celular con una clinica. Inmutable: cada paso produce una copia nueva.
 * {@code offeredOptions} son las unicas opciones validas en el estado actual.
 */
public record Conversation(
        UUID id,
        UUID clinicId,
        String phone,
        ConversationState state,
        UUID patientId,
        String patientName,
        UUID doctorStaffId,
        String doctorName,
        UUID requestId,
        List<ConversationOption> offeredOptions,
        int unrecognizedCount,
        LocalDateTime createdAt,
        LocalDateTime lastActivityAt
) {

    public Conversation {
        offeredOptions = offeredOptions == null ? List.of() : List.copyOf(offeredOptions);
    }

    public static Conversation start(UUID clinicId, String phone, LocalDateTime now) {
        return new Conversation(UUID.randomUUID(), clinicId, phone, ConversationState.IDENTIFICANDO,
                null, null, null, null, null, List.of(), 0, now, now);
    }
}
