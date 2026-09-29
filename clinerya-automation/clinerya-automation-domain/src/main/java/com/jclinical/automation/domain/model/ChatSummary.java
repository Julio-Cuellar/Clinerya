package com.jclinical.automation.domain.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Un hilo en la lista de chats. No lleva contenido: leer el contenido queda auditado.
 * {@code humanAttention}: lo atiende una persona desde {@code attentionSince}; {@code attentionUserId}
 * es quien lo tomo (null: lo pidio el agente). {@code profileName}: su nombre de perfil de WhatsApp.
 */
public record ChatSummary(String phone, List<String> patientNames, LocalDateTime lastMessageAt, int messageCount,
                          boolean humanAttention, UUID attentionUserId, LocalDateTime attentionSince,
                          String profileName) {

    public ChatSummary {
        patientNames = patientNames == null ? List.of() : List.copyOf(patientNames);
    }

    public ChatSummary(String phone, List<String> patientNames, LocalDateTime lastMessageAt, int messageCount) {
        this(phone, patientNames, lastMessageAt, messageCount, false, null, null, null);
    }
}
