package com.jclinical.automation.domain.ports.out;

import com.jclinical.automation.domain.model.ChatAttentionEvent;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Bitacora de atencion humana de los chats. */
public interface ChatAttentionLogPort {

    void record(ChatAttentionEvent event);

    Optional<ChatAttentionEvent> latest(UUID clinicId, String phone);

    /** El ultimo evento de cada chat indicado que tenga alguno. */
    Map<String, ChatAttentionEvent> latest(UUID clinicId, Collection<String> phones);
}
