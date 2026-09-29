package com.jclinical.automation.domain.ports.in;

import com.jclinical.automation.domain.model.ChatContact;

import java.util.UUID;

/** Ficha breve del contacto de un chat; la ve quien puede leer los chats (VIEW_PATIENTS). */
public interface ViewChatContactUseCase {

    ChatContact contact(UUID actingUserId, UUID clinicId, String phone);
}
