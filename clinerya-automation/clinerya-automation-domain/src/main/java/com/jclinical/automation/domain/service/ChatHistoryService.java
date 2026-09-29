package com.jclinical.automation.domain.service;

import com.jclinical.automation.domain.model.ChannelSettings;
import com.jclinical.automation.domain.model.ChatAccess;
import com.jclinical.automation.domain.model.ChatAttentionEvent;
import com.jclinical.automation.domain.ports.out.ChatAttentionLogPort;
import com.jclinical.automation.domain.ports.out.ChatContactsPort;
import com.jclinical.automation.domain.model.ChatMessage;
import com.jclinical.automation.domain.model.ChatSummary;
import com.jclinical.automation.domain.ports.in.PurgeChatHistoryUseCase;
import com.jclinical.automation.domain.ports.in.ReadChatHistoryUseCase;
import com.jclinical.automation.domain.ports.out.ChannelSettingsRepositoryPort;
import com.jclinical.automation.domain.ports.out.ChatAccessLogPort;
import com.jclinical.automation.domain.ports.out.ChatHistoryPort;
import com.jclinical.automation.domain.ports.out.PatientDirectoryPort;
import com.jclinical.automation.domain.ports.out.PatientDirectoryPort.PatientContact;
import com.jclinical.core.security.ClinicAccessDeniedException;
import com.jclinical.core.security.StaffPermission;
import com.jclinical.core.security.StaffPermissionCheckerPort;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Historial de WhatsApp de la clinica (D9). La lista de chats no muestra contenido; leer un chat
 * exige VIEW_PATIENTS y deja registro de quien y cuando; ese registro solo lo consulta el
 * administrador. La purga respeta la retencion de cada clinica.
 */
public class ChatHistoryService implements ReadChatHistoryUseCase, PurgeChatHistoryUseCase {

    static final int DEFAULT_PAGE = 50;
    static final int MAX_PAGE = 100;
    static final int MAX_CHATS = 200;
    static final int MAX_ACCESS_ROWS = 500;
    /** Seguir un chat abierto no vuelve a auditarse si esa persona lo leyo en este lapso. */
    static final Duration READ_AGAIN_AFTER = Duration.ofMinutes(30);

    private final ChatHistoryPort history;
    private final ChatAccessLogPort accessLog;
    private final PatientDirectoryPort patients;
    private final ChannelSettingsRepositoryPort settings;
    private final StaffPermissionCheckerPort permissions;
    private final Clock clock;
    private final ChatAttentionLogPort attentionLog;
    private final ChatContactsPort contacts;

    public ChatHistoryService(ChatHistoryPort history, ChatAccessLogPort accessLog, PatientDirectoryPort patients,
                              ChannelSettingsRepositoryPort settings, StaffPermissionCheckerPort permissions, Clock clock,
                              ChatAttentionLogPort attentionLog, ChatContactsPort contacts) {
        this.history = history;
        this.accessLog = accessLog;
        this.patients = patients;
        this.settings = settings;
        this.permissions = permissions;
        this.clock = clock;
        this.attentionLog = attentionLog;
        this.contacts = contacts;
    }

    public ChatHistoryService(ChatHistoryPort history, ChatAccessLogPort accessLog, PatientDirectoryPort patients,
                              ChannelSettingsRepositoryPort settings, StaffPermissionCheckerPort permissions, Clock clock,
                              ChatAttentionLogPort attentionLog) {
        this(history, accessLog, patients, settings, permissions, clock, attentionLog, null);
    }

    public ChatHistoryService(ChatHistoryPort history, ChatAccessLogPort accessLog, PatientDirectoryPort patients,
                              ChannelSettingsRepositoryPort settings, StaffPermissionCheckerPort permissions, Clock clock) {
        this(history, accessLog, patients, settings, permissions, clock, null, null);
    }

    /** Cada chat lleva su nombre de paciente y, si lo atiende una persona, quien lo tomo y desde cuando. */
    @Override
    public List<ChatSummary> listChats(UUID actingUserId, UUID clinicId) {
        requireChatAccess(actingUserId, clinicId);
        List<ChatSummary> chats = history.findChats(clinicId, MAX_CHATS);
        List<String> phones = chats.stream().map(ChatSummary::phone).toList();
        Map<String, ChatAttentionEvent> attention = attentionLog == null ? Map.of() : attentionLog.latest(clinicId, phones);
        Map<String, String> profileNames = contacts == null ? Map.of() : contacts.profileNames(clinicId, phones);
        return chats.stream().map(chat -> {
            List<String> names = patients.findByPhone(clinicId, chat.phone()).stream().map(PatientContact::displayName).toList();
            ChatAttentionEvent latest = attention.get(chat.phone());
            boolean human = latest != null && latest.action().human();
            return new ChatSummary(chat.phone(), names, chat.lastMessageAt(), chat.messageCount(), human,
                    human ? latest.userId() : null, human ? latest.at() : null, profileNames.get(chat.phone()));
        }).toList();
    }

    @Override
    public List<ChatMessage> readMessages(UUID actingUserId, UUID clinicId, String phone, LocalDateTime before, Integer limit) {
        requireChatAccess(actingUserId, clinicId);
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("Indica el chat que quieres leer.");
        }
        int page = limit == null ? DEFAULT_PAGE : Math.max(1, Math.min(limit, MAX_PAGE));
        List<ChatMessage> messages = history.findMessages(clinicId, phone, before, page);
        accessLog.record(new ChatAccess(UUID.randomUUID(), clinicId, phone, actingUserId, LocalDateTime.now(clock)));
        return messages;
    }

    @Override
    public List<ChatMessage> readNewMessages(UUID actingUserId, UUID clinicId, String phone, LocalDateTime after) {
        requireChatAccess(actingUserId, clinicId);
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("Indica el chat que quieres leer.");
        }
        if (after == null) {
            throw new IllegalArgumentException("Indica desde qué mensaje quieres leer.");
        }
        List<ChatMessage> messages = history.findMessagesAfter(clinicId, phone, after, MAX_PAGE);
        LocalDateTime now = LocalDateTime.now(clock);
        boolean readRecently = !accessLog.find(clinicId, phone, actingUserId, now.minus(READ_AGAIN_AFTER), null, 1).isEmpty();
        if (!readRecently) {
            accessLog.record(new ChatAccess(UUID.randomUUID(), clinicId, phone, actingUserId, now));
        }
        return messages;
    }

    @Override
    public List<ChatAccess> accessLog(UUID actingUserId, UUID clinicId, String phone, UUID userId,
                                      LocalDateTime from, LocalDateTime to) {
        if (!can(actingUserId, clinicId, StaffPermission.MANAGE_CLINIC)) {
            throw new ClinicAccessDeniedException("Solo el administrador de la clínica puede ver quién leyó los chats.");
        }
        String chat = phone == null || phone.isBlank() ? null : phone.trim();
        return accessLog.find(clinicId, chat, userId, from, to, MAX_ACCESS_ROWS);
    }

    @Override
    public int purgeExpired() {
        LocalDateTime now = LocalDateTime.now(clock);
        int deleted = 0;
        for (ChannelSettings clinic : settings.findAll()) {
            deleted += history.deleteOlderThan(clinic.clinicId(), now.minusMonths(clinic.chatRetentionMonths()));
        }
        return deleted;
    }

    private void requireChatAccess(UUID actingUserId, UUID clinicId) {
        if (!can(actingUserId, clinicId, StaffPermission.VIEW_PATIENTS)) {
            throw new ClinicAccessDeniedException("No tienes permiso para ver los chats de WhatsApp de esta clínica.");
        }
    }

    private boolean can(UUID actingUserId, UUID clinicId, StaffPermission permission) {
        return actingUserId != null && permissions.hasPermission(clinicId, actingUserId, permission);
    }
}
