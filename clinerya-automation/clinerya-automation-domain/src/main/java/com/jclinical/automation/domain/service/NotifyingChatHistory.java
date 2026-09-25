package com.jclinical.automation.domain.service;

import com.jclinical.automation.domain.model.ChatMessage;
import com.jclinical.automation.domain.model.ChatSummary;
import com.jclinical.automation.domain.ports.out.ChatHistoryPort;
import com.jclinical.automation.domain.ports.out.RealtimeNotifierPort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class NotifyingChatHistory implements ChatHistoryPort {

    public NotifyingChatHistory(ChatHistoryPort history, RealtimeNotifierPort notifier) {
    }

    @Override
    public void record(ChatMessage message) {
        throw new UnsupportedOperationException("pendiente");
    }

    @Override
    public List<ChatSummary> findChats(UUID clinicId, int limit) {
        throw new UnsupportedOperationException("pendiente");
    }

    @Override
    public List<ChatMessage> findMessages(UUID clinicId, String phone, LocalDateTime before, int limit) {
        throw new UnsupportedOperationException("pendiente");
    }

    @Override
    public int deleteOlderThan(UUID clinicId, LocalDateTime cutoff) {
        throw new UnsupportedOperationException("pendiente");
    }
}
