package com.jclinical.automation.infra.config;

import com.jclinical.automation.domain.model.InboundMessage;
import com.jclinical.automation.domain.model.OutboundReply;
import com.jclinical.automation.domain.ports.in.HandleInboundMessageUseCase;
import com.jclinical.automation.domain.service.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Un mensaje del paciente puede apartar un cupo, crear la solicitud y mover la conversacion: se
 * guarda todo o nada. Lo usara el canal de WhatsApp (entrega 5).
 */
@Service
@Primary
@RequiredArgsConstructor
public class TransactionalConversationUseCase implements HandleInboundMessageUseCase {

    private final ConversationService conversations;

    @Override
    @Transactional
    public List<OutboundReply> handle(InboundMessage message) {
        return conversations.handle(message);
    }
}
