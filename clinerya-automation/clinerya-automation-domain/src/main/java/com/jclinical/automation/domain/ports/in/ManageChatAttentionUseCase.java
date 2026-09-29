package com.jclinical.automation.domain.ports.in;

import com.jclinical.automation.domain.model.ChatAttention;

import java.util.UUID;

/**
 * Atencion humana en Chats (fase G): el personal que puede leer los chats (VIEW_PATIENTS) toma uno,
 * le escribe al paciente mientras WhatsApp lo permita y lo regresa al agente.
 */
public interface ManageChatAttentionUseCase {

    ChatAttention attention(UUID actingUserId, UUID clinicId, String phone);

    ChatAttention takeOver(UUID actingUserId, UUID clinicId, String phone);

    ChatAttention release(UUID actingUserId, UUID clinicId, String phone);

    void sendMessage(UUID actingUserId, UUID clinicId, String phone, String text);
}
