package com.jclinical.messaging.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;

public class OutboxRelay {

    public OutboxRelay(OutboxStore store, OutboxEventSender sender, ObjectMapper objectMapper) {
    }

    public int relay(int batchSize) {
        throw new UnsupportedOperationException("pendiente");
    }
}
