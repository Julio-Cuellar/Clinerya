package com.jclinical.automation.domain.service;

import com.jclinical.automation.domain.model.ChatMessage;
import com.jclinical.automation.domain.model.ConversationOption;
import com.jclinical.automation.domain.model.PatientNotification;
import com.jclinical.automation.domain.ports.out.ChatHistoryPort;
import com.jclinical.automation.domain.ports.out.OutboundMessageQueuePort;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Todo lo que sale hacia el paciente pasa por aqui: queda en su historial de chat, tal como lo vio
 * (texto y opciones), y se encola para enviarse. Un solo punto para que nada salga sin registro.
 */
public class RecordingOutboundQueue implements OutboundMessageQueuePort {

    private final OutboundMessageQueuePort queue;
    private final ChatHistoryPort history;
    private final Clock clock;

    public RecordingOutboundQueue(OutboundMessageQueuePort queue, ChatHistoryPort history, Clock clock) {
        this.queue = queue;
        this.history = history;
        this.clock = clock;
    }

    @Override
    public void enqueue(PatientNotification notification) {
        history.record(new ChatMessage(UUID.randomUUID(), notification.clinicId(), notification.phone(),
                ChatMessage.Direction.OUTBOUND, notification.reply().text(),
                notification.reply().options().stream().map(ConversationOption::label).toList(), LocalDateTime.now(clock)));
        queue.enqueue(notification);
    }
}
