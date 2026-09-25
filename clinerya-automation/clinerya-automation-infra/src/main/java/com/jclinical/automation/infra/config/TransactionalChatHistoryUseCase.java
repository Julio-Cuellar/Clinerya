package com.jclinical.automation.infra.config;

import com.jclinical.automation.domain.model.ChatAccess;
import com.jclinical.automation.domain.model.ChatMessage;
import com.jclinical.automation.domain.model.ChatSummary;
import com.jclinical.automation.domain.ports.in.PurgeChatHistoryUseCase;
import com.jclinical.automation.domain.ports.in.ReadChatHistoryUseCase;
import com.jclinical.automation.domain.service.ChatHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Leer un chat y registrar esa lectura van en la misma transaccion: no hay lectura sin rastro. */
@Service
@Primary
@RequiredArgsConstructor
public class TransactionalChatHistoryUseCase implements ReadChatHistoryUseCase, PurgeChatHistoryUseCase {

    private final ChatHistoryService chats;

    @Override
    @Transactional(readOnly = true)
    public List<ChatSummary> listChats(UUID actingUserId, UUID clinicId) {
        return chats.listChats(actingUserId, clinicId);
    }

    @Override
    @Transactional
    public List<ChatMessage> readMessages(UUID actingUserId, UUID clinicId, String phone, LocalDateTime before, Integer limit) {
        return chats.readMessages(actingUserId, clinicId, phone, before, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatAccess> accessLog(UUID actingUserId, UUID clinicId, String phone, UUID userId,
                                      LocalDateTime from, LocalDateTime to) {
        return chats.accessLog(actingUserId, clinicId, phone, userId, from, to);
    }

    @Override
    @Transactional
    public int purgeExpired() {
        return chats.purgeExpired();
    }
}
