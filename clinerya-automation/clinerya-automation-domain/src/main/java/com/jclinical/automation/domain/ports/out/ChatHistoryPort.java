package com.jclinical.automation.domain.ports.out;

import com.jclinical.automation.domain.model.ChatMessage;
import com.jclinical.automation.domain.model.ChatSummary;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatHistoryPort {

    /** Cuantos mensajes recientes revisa la implementacion por omision de {@link #lastInboundAt}. */
    int RECENT_FOR_LAST_INBOUND = 100;

    void record(ChatMessage message);

    /** Cuando escribio el paciente por ultima vez (abre la ventana de 24 h de WhatsApp). */
    default Optional<LocalDateTime> lastInboundAt(UUID clinicId, String phone) {
        return findMessages(clinicId, phone, null, RECENT_FOR_LAST_INBOUND).stream()
                .filter(message -> message.direction() == ChatMessage.Direction.INBOUND)
                .map(ChatMessage::at)
                .max(LocalDateTime::compareTo);
    }

    /** Hilos de la clinica, el mas reciente primero (sin contenido). */
    List<ChatSummary> findChats(UUID clinicId, int limit);

    /** Mensajes del hilo anteriores a {@code before} (o los ultimos si es null), el mas reciente primero. */
    List<ChatMessage> findMessages(UUID clinicId, String phone, LocalDateTime before, int limit);

    /** Mensajes del hilo posteriores a {@code after}, el mas antiguo primero (para seguir el chat en vivo). */
    List<ChatMessage> findMessagesAfter(UUID clinicId, String phone, LocalDateTime after, int limit);

    int deleteOlderThan(UUID clinicId, LocalDateTime cutoff);
}
