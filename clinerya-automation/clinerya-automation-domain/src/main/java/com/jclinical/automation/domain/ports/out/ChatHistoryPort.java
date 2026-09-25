package com.jclinical.automation.domain.ports.out;

import com.jclinical.automation.domain.model.ChatMessage;
import com.jclinical.automation.domain.model.ChatSummary;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ChatHistoryPort {

    void record(ChatMessage message);

    /** Hilos de la clinica, el mas reciente primero (sin contenido). */
    List<ChatSummary> findChats(UUID clinicId, int limit);

    /** Mensajes del hilo anteriores a {@code before} (o los ultimos si es null), el mas reciente primero. */
    List<ChatMessage> findMessages(UUID clinicId, String phone, LocalDateTime before, int limit);

    int deleteOlderThan(UUID clinicId, LocalDateTime cutoff);
}
