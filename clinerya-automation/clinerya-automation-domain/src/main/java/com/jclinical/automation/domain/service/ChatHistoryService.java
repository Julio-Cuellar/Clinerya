package com.jclinical.automation.domain.service;

import com.jclinical.automation.domain.model.ChatAccess;
import com.jclinical.automation.domain.model.ChatMessage;
import com.jclinical.automation.domain.model.ChatSummary;
import com.jclinical.automation.domain.ports.in.PurgeChatHistoryUseCase;
import com.jclinical.automation.domain.ports.in.ReadChatHistoryUseCase;
import com.jclinical.automation.domain.ports.out.ChannelSettingsRepositoryPort;
import com.jclinical.automation.domain.ports.out.ChatAccessLogPort;
import com.jclinical.automation.domain.ports.out.ChatHistoryPort;
import com.jclinical.automation.domain.ports.out.PatientDirectoryPort;
import com.jclinical.core.security.StaffPermissionCheckerPort;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class ChatHistoryService implements ReadChatHistoryUseCase, PurgeChatHistoryUseCase {

    static final int MAX_PAGE = 100;

    public ChatHistoryService(ChatHistoryPort history, ChatAccessLogPort accessLog, PatientDirectoryPort patients,
                              ChannelSettingsRepositoryPort settings, StaffPermissionCheckerPort permissions, Clock clock) {
    }

    @Override
    public List<ChatSummary> listChats(UUID actingUserId, UUID clinicId) {
        throw new UnsupportedOperationException("pendiente");
    }

    @Override
    public List<ChatMessage> readMessages(UUID actingUserId, UUID clinicId, String phone, LocalDateTime before, Integer limit) {
        throw new UnsupportedOperationException("pendiente");
    }

    @Override
    public List<ChatAccess> accessLog(UUID actingUserId, UUID clinicId, String phone, UUID userId, LocalDateTime from, LocalDateTime to) {
        throw new UnsupportedOperationException("pendiente");
    }

    @Override
    public int purgeExpired() {
        throw new UnsupportedOperationException("pendiente");
    }
}
