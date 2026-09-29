package com.jclinical.automation.infra.config;

import com.jclinical.automation.domain.model.ChatAttention;
import com.jclinical.automation.domain.ports.in.ManageChatAttentionUseCase;
import com.jclinical.automation.domain.service.ChatAttentionService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Tomar o regresar un chat y su registro en la bitacora van juntos; el mensaje y su cola, tambien. */
@Service
@Primary
@RequiredArgsConstructor
public class TransactionalChatAttentionUseCase implements ManageChatAttentionUseCase {

    private final ChatAttentionService attention;

    @Override
    @Transactional(readOnly = true)
    public ChatAttention attention(UUID actingUserId, UUID clinicId, String phone) {
        return attention.attention(actingUserId, clinicId, phone);
    }

    @Override
    @Transactional
    public ChatAttention takeOver(UUID actingUserId, UUID clinicId, String phone) {
        return attention.takeOver(actingUserId, clinicId, phone);
    }

    @Override
    @Transactional
    public ChatAttention release(UUID actingUserId, UUID clinicId, String phone) {
        return attention.release(actingUserId, clinicId, phone);
    }

    @Override
    @Transactional
    public void sendMessage(UUID actingUserId, UUID clinicId, String phone, String text) {
        attention.sendMessage(actingUserId, clinicId, phone, text);
    }
}
