package com.jclinical.messaging.outbox;

@FunctionalInterface
public interface OutboxEventSender {
    void send(String routingKey, Object payload);
}
