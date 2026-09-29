package com.jclinical.automation.domain.service;

import com.jclinical.automation.domain.model.ChatMessage;
import com.jclinical.automation.domain.model.ChatSummary;
import com.jclinical.automation.domain.ports.out.ChatHistoryPort;
import com.jclinical.automation.domain.ports.out.RealtimeNotifierPort;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Cada mensaje guardado en el historial avisa que ese chat tuvo actividad, sin mandar su contenido. */
class NotifyingChatHistoryTest {

    private static final LocalDateTime AT = LocalDateTime.of(2026, 9, 25, 10, 0);

    private final UUID clinicId = UUID.randomUUID();
    private final List<ChatMessage> stored = new ArrayList<>();
    private final RecordingNotifier notifier = new RecordingNotifier();

    private final NotifyingChatHistory history = new NotifyingChatHistory(new ChatHistoryPort() {
        @Override
        public void record(ChatMessage message) {
            stored.add(message);
        }

        @Override
        public List<ChatSummary> findChats(UUID clinic, int limit) {
            return List.of(new ChatSummary("5215512345678", List.of(), AT, 1));
        }

        @Override
        public List<ChatMessage> findMessages(UUID clinic, String phone, LocalDateTime before, int limit) {
            return stored;
        }

        @Override
        public List<ChatMessage> findMessagesAfter(UUID clinic, String phone, LocalDateTime after, int limit) {
            return List.of();
        }

        @Override
        public int deleteOlderThan(UUID clinic, LocalDateTime cutoff) {
            return 7;
        }

        @Override
        public Optional<LocalDateTime> lastInboundAt(UUID clinic, String phone) {
            return Optional.of(AT.minusHours(1));
        }
    }, notifier);

    @Test
    void theLastPatientMessageComesFromTheStoredHistory() {
        assertEquals(Optional.of(AT.minusHours(1)), history.lastInboundAt(clinicId, "5215512345678"));
    }

    @Test
    void recordingAMessageAnnouncesActivityInThatChat() {
        ChatMessage message = new ChatMessage(UUID.randomUUID(), clinicId, "5215512345678",
                ChatMessage.Direction.INBOUND, "Hola", List.of(), AT);

        history.record(message);

        assertEquals(List.of(message), stored);
        assertEquals(List.of(clinicId + "|5215512345678|" + AT), notifier.chatActivity);
    }

    @Test
    void readsAndPurgesGoStraightToTheHistory() {
        assertEquals(1, history.findChats(clinicId, 10).size());
        assertEquals(7, history.deleteOlderThan(clinicId, AT));
        assertTrue(notifier.chatActivity.isEmpty());
    }

    static final class RecordingNotifier implements RealtimeNotifierPort {
        final List<String> chatActivity = new ArrayList<>();
        final List<String> newRequests = new ArrayList<>();

        @Override
        public void chatActivity(UUID clinicId, String phone, LocalDateTime at) {
            chatActivity.add(clinicId + "|" + phone + "|" + at);
        }

        @Override
        public void newAppointmentRequest(UUID clinicId, UUID doctorStaffId, UUID requestId) {
            newRequests.add(clinicId + "|" + doctorStaffId + "|" + requestId);
        }
    }
}
