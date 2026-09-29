package com.jclinical.automation.domain.service;

import com.jclinical.automation.domain.model.ChatAttention;
import com.jclinical.automation.domain.model.ChatAttentionEvent;
import com.jclinical.automation.domain.model.ChatAttentionEvent.Action;
import com.jclinical.automation.domain.model.ChatMessage;
import com.jclinical.automation.domain.model.ChatMessage.Direction;
import com.jclinical.automation.domain.model.ChatSummary;
import com.jclinical.automation.domain.model.Conversation;
import com.jclinical.automation.domain.model.ConversationState;
import com.jclinical.automation.domain.model.PatientNotification;
import com.jclinical.automation.domain.ports.out.ChatAccessLogPort;
import com.jclinical.automation.domain.ports.out.ChatAttentionLogPort;
import com.jclinical.automation.domain.ports.out.RealtimeNotifierPort;
import com.jclinical.automation.domain.service.ChannelSettingsServiceTest.InMemorySettings;
import com.jclinical.automation.domain.service.ChatHistoryServiceTest.InMemoryHistory;
import com.jclinical.automation.domain.service.ConversationServiceTest.FakePatients;
import com.jclinical.automation.domain.service.ConversationServiceTest.InMemoryConversations;
import com.jclinical.core.security.ClinicAccessDeniedException;
import com.jclinical.core.security.StaffPermission;
import com.jclinical.core.security.StaffPermissionCheckerPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fase G: alguien de la clinica toma un chat ("Atencion humana"), el agente calla, el personal le
 * escribe al paciente desde Chats dentro de la ventana de 24 h de WhatsApp y despues lo regresa al
 * agente. Queda registrado quien lo tomo, quien lo regreso y cuando.
 */
class ChatAttentionServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 29, 12, 0);
    private static final String PHONE = "5215512345678";

    private final UUID clinicId = UUID.randomUUID();
    private final UUID receptionistId = UUID.randomUUID();
    private final UUID strangerId = UUID.randomUUID();

    private final InMemoryConversations conversations = new InMemoryConversations();
    private final InMemoryHistory history = new InMemoryHistory();
    private final InMemoryAttentionLog attentionLog = new InMemoryAttentionLog();
    private final List<PatientNotification> sent = new ArrayList<>();
    private final List<String> signals = new ArrayList<>();
    private final Clock clock = Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    private ChatAttentionService service;

    @BeforeEach
    void setUp() {
        StaffPermissionCheckerPort permissions = (clinic, user, permission) ->
                user.equals(receptionistId) && permission == StaffPermission.VIEW_PATIENTS;
        RealtimeNotifierPort realtime = new RealtimeNotifierPort() {
            @Override
            public void chatActivity(UUID clinic, String phone, LocalDateTime at) {
                signals.add(phone);
            }

            @Override
            public void newAppointmentRequest(UUID clinic, UUID doctorStaffId, UUID requestId) {
            }
        };
        service = new ChatAttentionService(conversations, history, new RecordingOutboundQueue(sent::add, history, clock),
                attentionLog, realtime, permissions, clock);
    }

    @Test
    void takingAChatSilencesTheAgentAndRecordsWhoTookIt() {
        conversations.save(conversation(ConversationState.CONVERSANDO, NOW.minusMinutes(2)));

        ChatAttention attention = service.takeOver(receptionistId, clinicId, PHONE);

        assertEquals(ConversationState.ATENCION_HUMANA, conversations.findActive(clinicId, PHONE).orElseThrow().state());
        assertTrue(attention.human());
        assertEquals(receptionistId, attention.byUserId());
        assertEquals(NOW, attention.since());
        assertEquals(Action.TAKEN, attentionLog.events.getLast().action());
        assertEquals(receptionistId, attentionLog.events.getLast().userId());
        assertEquals(List.of(PHONE), signals, "las pantallas abiertas se actualizan");
    }

    @Test
    void aChatWithoutAnActiveConversationCanAlsoBeTaken() {
        service.takeOver(receptionistId, clinicId, PHONE);

        Conversation opened = conversations.findActive(clinicId, PHONE).orElseThrow();
        assertEquals(ConversationState.ATENCION_HUMANA, opened.state());
        assertEquals(NOW, opened.lastActivityAt());
    }

    @Test
    void returningTheChatHandsItBackToTheAgentWithAFreshStart() {
        Conversation human = conversation(ConversationState.ATENCION_HUMANA, NOW.minusMinutes(5));
        conversations.save(new Conversation(human.id(), clinicId, PHONE, human.state(), null, null, null, null, null,
                List.of(), 2, human.createdAt(), human.lastActivityAt()));

        ChatAttention attention = service.release(receptionistId, clinicId, PHONE);

        Conversation back = conversations.findActive(clinicId, PHONE).orElseThrow();
        assertEquals(ConversationState.CONVERSANDO, back.state());
        assertEquals(0, back.unrecognizedCount(), "el agente no arrastra los malentendidos que llevaron a la persona");
        assertFalse(attention.human());
        assertEquals(Action.RELEASED, attentionLog.events.getLast().action());
        assertEquals(receptionistId, attentionLog.events.getLast().userId());
    }

    @Test
    void theStaffWritesToThePatientAndTheMessageIsSignedAndKept() {
        conversations.save(conversation(ConversationState.ATENCION_HUMANA, NOW.minusMinutes(1)));
        history.record(inbound("¿Me dan factura?", NOW.minusHours(3)));

        service.sendMessage(receptionistId, clinicId, PHONE, "  Hola Ana, soy María de recepción.  ");

        PatientNotification notification = sent.getFirst();
        assertEquals("Hola Ana, soy María de recepción.", notification.reply().text());
        assertEquals(receptionistId, notification.authorUserId());
        ChatMessage kept = history.messages.getLast();
        assertEquals(Direction.STAFF, kept.direction());
        assertEquals(receptionistId, kept.authorUserId());
        assertEquals(NOW, conversations.findActive(clinicId, PHONE).orElseThrow().lastActivityAt(),
                "escribir cuenta como actividad del chat");
    }

    @Test
    void theStaffCannotWriteWhileTheAgentIsAttending() {
        conversations.save(conversation(ConversationState.CONVERSANDO, NOW.minusMinutes(1)));
        history.record(inbound("Hola", NOW.minusMinutes(1)));

        assertThrows(IllegalStateException.class, () -> service.sendMessage(receptionistId, clinicId, PHONE, "Hola"));
        assertTrue(sent.isEmpty());
    }

    @Test
    void afterTwentyFourHoursWithoutThePatientWritingWhatsAppDoesNotAllowFreeText() {
        conversations.save(conversation(ConversationState.ATENCION_HUMANA, NOW.minusMinutes(1)));
        history.record(inbound("¿Me dan factura?", NOW.minusHours(24).minusMinutes(1)));

        assertThrows(IllegalStateException.class, () -> service.sendMessage(receptionistId, clinicId, PHONE, "Hola"));
        assertTrue(sent.isEmpty());
    }

    @Test
    void anEmptyOrTooLongMessageIsRejected() {
        conversations.save(conversation(ConversationState.ATENCION_HUMANA, NOW.minusMinutes(1)));
        history.record(inbound("Hola", NOW.minusMinutes(1)));

        assertThrows(IllegalArgumentException.class, () -> service.sendMessage(receptionistId, clinicId, PHONE, "   "));
        assertThrows(IllegalArgumentException.class, () -> service.sendMessage(receptionistId, clinicId, PHONE,
                "a".repeat(ChatAttentionService.MAX_MESSAGE_LENGTH + 1)));
        assertTrue(sent.isEmpty());
    }

    @Test
    void onlyStaffWhoCanReadChatsCanTakeWriteOrReturnThem() {
        conversations.save(conversation(ConversationState.ATENCION_HUMANA, NOW.minusMinutes(1)));
        history.record(inbound("Hola", NOW.minusMinutes(1)));

        assertThrows(ClinicAccessDeniedException.class, () -> service.takeOver(strangerId, clinicId, PHONE));
        assertThrows(ClinicAccessDeniedException.class, () -> service.release(strangerId, clinicId, PHONE));
        assertThrows(ClinicAccessDeniedException.class, () -> service.sendMessage(strangerId, clinicId, PHONE, "Hola"));
        assertThrows(ClinicAccessDeniedException.class, () -> service.attention(strangerId, clinicId, PHONE));
        assertTrue(sent.isEmpty());
    }

    @Test
    void theOpenChatShowsWhoAskedForHelpAndUntilWhenTheStaffCanReply() {
        conversations.save(conversation(ConversationState.ATENCION_HUMANA, NOW.minusMinutes(10)));
        history.record(inbound("Quiero hablar con una persona", NOW.minusMinutes(10)));
        attentionLog.record(new ChatAttentionEvent(UUID.randomUUID(), clinicId, PHONE, Action.REQUESTED_BY_AGENT, null,
                NOW.minusMinutes(10)));

        ChatAttention attention = service.attention(receptionistId, clinicId, PHONE);

        assertTrue(attention.human());
        assertNull(attention.byUserId(), "la pidio el agente");
        assertEquals(NOW.minusMinutes(10), attention.since());
        assertEquals(NOW.minusMinutes(10).plusHours(24), attention.replyUntil());
    }

    @Test
    void theChatListMarksTheChatsThatAPersonIsAttending() {
        history.record(inbound("Hola", NOW.minusMinutes(3)));
        history.record(new ChatMessage(UUID.randomUUID(), clinicId, "5215599999999", Direction.INBOUND, "Hola", List.of(),
                NOW.minusMinutes(4)));
        attentionLog.record(new ChatAttentionEvent(UUID.randomUUID(), clinicId, PHONE, Action.TAKEN, receptionistId,
                NOW.minusMinutes(2)));
        ChatHistoryService chats = new ChatHistoryService(history, noAccessLog(), new FakePatients(), new InMemorySettings(),
                (clinic, user, permission) -> true, clock, attentionLog);

        Map<String, ChatSummary> byPhone = chats.listChats(receptionistId, clinicId).stream()
                .collect(Collectors.toMap(ChatSummary::phone, Function.identity()));

        assertTrue(byPhone.get(PHONE).humanAttention());
        assertEquals(receptionistId, byPhone.get(PHONE).attentionUserId());
        assertEquals(NOW.minusMinutes(2), byPhone.get(PHONE).attentionSince());
        assertFalse(byPhone.get("5215599999999").humanAttention());
    }

    private Conversation conversation(ConversationState state, LocalDateTime lastActivity) {
        return new Conversation(UUID.randomUUID(), clinicId, PHONE, state, null, null, null, null, null, List.of(), 0,
                lastActivity, lastActivity);
    }

    private ChatMessage inbound(String text, LocalDateTime at) {
        return new ChatMessage(UUID.randomUUID(), clinicId, PHONE, Direction.INBOUND, text, List.of(), at);
    }

    private static ChatAccessLogPort noAccessLog() {
        return new ChatAccessLogPort() {
            @Override
            public void record(com.jclinical.automation.domain.model.ChatAccess access) {
            }

            @Override
            public List<com.jclinical.automation.domain.model.ChatAccess> find(UUID clinicId, String phone, UUID userId,
                                                                              LocalDateTime from, LocalDateTime to, int limit) {
                return List.of();
            }
        };
    }

    static final class InMemoryAttentionLog implements ChatAttentionLogPort {
        final List<ChatAttentionEvent> events = new ArrayList<>();

        @Override
        public void record(ChatAttentionEvent event) {
            events.add(event);
        }

        @Override
        public Optional<ChatAttentionEvent> latest(UUID clinicId, String phone) {
            return events.stream().filter(e -> e.clinicId().equals(clinicId) && e.phone().equals(phone))
                    .max(Comparator.comparing(ChatAttentionEvent::at));
        }

        @Override
        public Map<String, ChatAttentionEvent> latest(UUID clinicId, Collection<String> phones) {
            return phones.stream().map(phone -> latest(clinicId, phone)).flatMap(Optional::stream)
                    .collect(Collectors.toMap(ChatAttentionEvent::phone, Function.identity()));
        }
    }
}
