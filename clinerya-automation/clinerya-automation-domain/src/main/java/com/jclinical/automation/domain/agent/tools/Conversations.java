package com.jclinical.automation.domain.agent.tools;

import com.jclinical.automation.domain.model.Conversation;
import com.jclinical.automation.domain.ports.out.ConversationRepositoryPort;

import java.util.UUID;

/** Cambios puntuales que una herramienta hace sobre la conversacion guardada. */
final class Conversations {

    private Conversations() {
    }

    static void setRequest(ConversationRepositoryPort conversations, UUID conversationId, UUID requestId) {
        conversations.findById(conversationId).ifPresent(current -> conversations.save(new Conversation(current.id(),
                current.clinicId(), current.phone(), current.state(), current.patientId(), current.patientName(),
                current.doctorStaffId(), current.doctorName(), requestId, current.offeredOptions(),
                current.unrecognizedCount(), current.createdAt(), current.lastActivityAt())));
    }
}
