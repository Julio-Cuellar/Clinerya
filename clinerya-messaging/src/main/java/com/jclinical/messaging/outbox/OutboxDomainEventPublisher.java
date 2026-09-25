package com.jclinical.messaging.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jclinical.core.events.DomainEventPublisherPort;

import java.time.Clock;

public class OutboxDomainEventPublisher implements DomainEventPublisherPort {

    public OutboxDomainEventPublisher(OutboxStore store, ObjectMapper objectMapper, Clock clock) {
    }

    @Override
    public void publish(String routingKey, Object payload) {
        throw new UnsupportedOperationException("pendiente");
    }
}
