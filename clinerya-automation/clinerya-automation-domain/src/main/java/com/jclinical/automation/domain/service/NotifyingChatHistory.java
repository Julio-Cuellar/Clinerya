package com.jclinical.automation.domain.service;

import com.jclinical.automation.domain.model.ChatMessage;
import com.jclinical.automation.domain.model.ChatSummary;
import com.jclinical.automation.domain.ports.out.ChatHistoryPort;
import com.jclinical.automation.domain.ports.out.RealtimeNotifierPort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Cada mensaje que entra al historial avisa a las pantallas abiertas que ese chat tuvo actividad. El
 * aviso no lleva el texto: quien quiera leerlo pasa por el endpoint auditado (D9).
 */
public class NotifyingChatHistory implements ChatHistoryPort {

    private final ChatHistoryPort history;
    private final RealtimeNotifierPort notifier;

    public NotifyingChatHistory(ChatHistoryPort history, RealtimeNotifierPort notifier) {
        this.history = history;
        this.notifier = notifier;
    }

    @Override
    public void record(ChatMessage message) {
        history.record(message);
        notifier.chatActivity(message.clinicId(), message.phone(), message.at());
    }

    @Override
    public List<ChatSummary> findChats(UUID clinicId, int limit) {
        return history.findChats(clinicId, limit);
    }

    @Override
    public List<ChatMessage> findMessages(UUID clinicId, String phone, LocalDateTime before, int limit) {
        return history.findMessages(clinicId, phone, before, limit);
    }

    @Override
    public List<ChatMessage> findMessagesAfter(UUID clinicId, String phone, LocalDateTime after, int limit) {
        return history.findMessagesAfter(clinicId, phone, after, limit);
    }

    @Override
    public int deleteOlderThan(UUID clinicId, LocalDateTime cutoff) {
        return history.deleteOlderThan(clinicId, cutoff);
    }
}
