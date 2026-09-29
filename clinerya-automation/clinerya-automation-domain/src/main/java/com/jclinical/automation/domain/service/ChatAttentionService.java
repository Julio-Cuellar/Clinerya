package com.jclinical.automation.domain.service;

import com.jclinical.automation.domain.model.ChatAttention;
import com.jclinical.automation.domain.model.ChatAttentionEvent;
import com.jclinical.automation.domain.model.ChatAttentionEvent.Action;
import com.jclinical.automation.domain.model.Conversation;
import com.jclinical.automation.domain.model.ConversationState;
import com.jclinical.automation.domain.model.OutboundReply;
import com.jclinical.automation.domain.model.PatientNotification;
import com.jclinical.automation.domain.ports.in.ManageChatAttentionUseCase;
import com.jclinical.automation.domain.ports.out.ChatAttentionLogPort;
import com.jclinical.automation.domain.ports.out.ChatHistoryPort;
import com.jclinical.automation.domain.ports.out.ConversationRepositoryPort;
import com.jclinical.automation.domain.ports.out.OutboundMessageQueuePort;
import com.jclinical.automation.domain.ports.out.RealtimeNotifierPort;
import com.jclinical.core.security.ClinicAccessDeniedException;
import com.jclinical.core.security.StaffPermission;
import com.jclinical.core.security.StaffPermissionCheckerPort;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Atencion humana en Chats (fase G). Mientras un chat esta en atencion humana el agente calla; el
 * personal le escribe al paciente solo dentro de la ventana de 24 h de WhatsApp (texto libre) y cada
 * mensaje sale firmado por quien lo escribio. Tomar y regresar un chat queda en la bitacora.
 */
public class ChatAttentionService implements ManageChatAttentionUseCase {

    /** Limite de WhatsApp para un mensaje de texto. */
    public static final int MAX_MESSAGE_LENGTH = 4096;
    public static final Duration REPLY_WINDOW = Duration.ofHours(24);

    private final ConversationRepositoryPort conversations;
    private final ChatHistoryPort history;
    private final OutboundMessageQueuePort outbound;
    private final ChatAttentionLogPort attentionLog;
    private final RealtimeNotifierPort realtime;
    private final StaffPermissionCheckerPort permissions;
    private final Clock clock;

    public ChatAttentionService(ConversationRepositoryPort conversations, ChatHistoryPort history,
                                OutboundMessageQueuePort outbound, ChatAttentionLogPort attentionLog,
                                RealtimeNotifierPort realtime, StaffPermissionCheckerPort permissions, Clock clock) {
        this.conversations = conversations;
        this.history = history;
        this.outbound = outbound;
        this.attentionLog = attentionLog;
        this.realtime = realtime;
        this.permissions = permissions;
        this.clock = clock;
    }

    @Override
    public ChatAttention attention(UUID actingUserId, UUID clinicId, String phone) {
        requireChatAccess(actingUserId, clinicId, phone);
        return current(clinicId, phone);
    }

    @Override
    public ChatAttention takeOver(UUID actingUserId, UUID clinicId, String phone) {
        requireChatAccess(actingUserId, clinicId, phone);
        LocalDateTime now = LocalDateTime.now(clock);
        Optional<Conversation> active = conversations.findActive(clinicId, phone);
        Conversation human = active
                .map(conversation -> copy(conversation, ConversationState.ATENCION_HUMANA, conversation.unrecognizedCount(), now))
                .orElseGet(() -> new Conversation(UUID.randomUUID(), clinicId, phone, ConversationState.ATENCION_HUMANA,
                        null, null, null, null, null, List.of(), 0, now, now));
        conversations.save(human);
        log(clinicId, phone, Action.TAKEN, actingUserId, now);
        return current(clinicId, phone);
    }

    @Override
    public ChatAttention release(UUID actingUserId, UUID clinicId, String phone) {
        requireChatAccess(actingUserId, clinicId, phone);
        LocalDateTime now = LocalDateTime.now(clock);
        Optional<Conversation> human = conversations.findActive(clinicId, phone)
                .filter(conversation -> conversation.state() == ConversationState.ATENCION_HUMANA);
        if (human.isPresent()) {
            conversations.save(copy(human.get(), ConversationState.CONVERSANDO, 0, now));
            log(clinicId, phone, Action.RELEASED, actingUserId, now);
        }
        return current(clinicId, phone);
    }

    @Override
    public void sendMessage(UUID actingUserId, UUID clinicId, String phone, String text) {
        requireChatAccess(actingUserId, clinicId, phone);
        String message = text == null ? "" : text.strip();
        if (message.isEmpty()) {
            throw new IllegalArgumentException("Escribe el mensaje para el paciente.");
        }
        if (message.length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("El mensaje es demasiado largo (máximo " + MAX_MESSAGE_LENGTH + " caracteres).");
        }
        Conversation human = conversations.findActive(clinicId, phone)
                .filter(conversation -> conversation.state() == ConversationState.ATENCION_HUMANA)
                .orElseThrow(() -> new IllegalStateException(
                        "Activa \"Atención humana\" para escribirle al paciente; mientras tanto responde el agente."));
        LocalDateTime now = LocalDateTime.now(clock);
        if (replyUntil(clinicId, phone).filter(now::isBefore).isEmpty()) {
            throw new IllegalStateException("WhatsApp solo permite escribirle dentro de las 24 horas siguientes a su último "
                    + "mensaje. Podrás responderle cuando vuelva a escribir.");
        }
        outbound.enqueue(new PatientNotification(clinicId, phone, OutboundReply.text(message), actingUserId));
        conversations.save(copy(human, human.state(), human.unrecognizedCount(), now));
    }

    private ChatAttention current(UUID clinicId, String phone) {
        boolean human = conversations.findActive(clinicId, phone)
                .map(conversation -> conversation.state() == ConversationState.ATENCION_HUMANA)
                .orElse(false);
        LocalDateTime replyUntil = replyUntil(clinicId, phone).orElse(null);
        if (!human) {
            return new ChatAttention(false, null, null, replyUntil);
        }
        Optional<ChatAttentionEvent> latest = attentionLog.latest(clinicId, phone).filter(event -> event.action().human());
        return new ChatAttention(true, latest.map(ChatAttentionEvent::at).orElse(null),
                latest.map(ChatAttentionEvent::userId).orElse(null), replyUntil);
    }

    private Optional<LocalDateTime> replyUntil(UUID clinicId, String phone) {
        return history.lastInboundAt(clinicId, phone).map(lastInbound -> lastInbound.plus(REPLY_WINDOW));
    }

    private void log(UUID clinicId, String phone, Action action, UUID userId, LocalDateTime at) {
        attentionLog.record(new ChatAttentionEvent(UUID.randomUUID(), clinicId, phone, action, userId, at));
        realtime.chatActivity(clinicId, phone, at);
    }

    private void requireChatAccess(UUID actingUserId, UUID clinicId, String phone) {
        if (actingUserId == null || !permissions.hasPermission(clinicId, actingUserId, StaffPermission.VIEW_PATIENTS)) {
            throw new ClinicAccessDeniedException("No tienes permiso para atender los chats de WhatsApp de esta clínica.");
        }
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("Indica el chat.");
        }
    }

    private static Conversation copy(Conversation conversation, ConversationState state, int unrecognized,
                                     LocalDateTime lastActivity) {
        return new Conversation(conversation.id(), conversation.clinicId(), conversation.phone(), state,
                conversation.patientId(), conversation.patientName(), conversation.doctorStaffId(), conversation.doctorName(),
                conversation.requestId(), conversation.offeredOptions(), unrecognized, conversation.createdAt(), lastActivity);
    }
}
