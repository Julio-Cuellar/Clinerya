package com.jclinical.automation.domain.service;

import com.jclinical.automation.domain.model.PatientNotification;
import com.jclinical.automation.domain.ports.out.ChatHistoryPort;
import com.jclinical.automation.domain.ports.out.OutboundMessageQueuePort;

import java.time.Clock;

public class RecordingOutboundQueue implements OutboundMessageQueuePort {

    public RecordingOutboundQueue(OutboundMessageQueuePort queue, ChatHistoryPort history, Clock clock) {
    }

    @Override
    public void enqueue(PatientNotification notification) {
        throw new UnsupportedOperationException("pendiente");
    }
}
