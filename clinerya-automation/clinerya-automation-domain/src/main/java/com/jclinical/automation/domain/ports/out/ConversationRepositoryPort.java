package com.jclinical.automation.domain.ports.out;

import com.jclinical.automation.domain.model.Conversation;

import java.util.Optional;
import java.util.UUID;

public interface ConversationRepositoryPort {
    /** La conversacion no terminal de ese celular en esa clinica, si existe. */
    Optional<Conversation> findActive(UUID clinicId, String phone);

    Conversation save(Conversation conversation);
}
