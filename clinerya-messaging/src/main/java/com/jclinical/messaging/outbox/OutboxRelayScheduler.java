package com.jclinical.messaging.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Slf4j
public class OutboxRelayScheduler {

    private final OutboxRelay relay;

    @Value("${app.messaging.outbox.batch-size:100}")
    private int batchSize;

    @Scheduled(
            fixedDelayString = "${app.messaging.outbox.dispatch-delay-ms:500}",
            initialDelayString = "${app.messaging.outbox.initial-delay-ms:2000}")
    @Transactional
    public void relayPending() {
        int delivered = relay.relay(batchSize);
        if (delivered > 0) {
            log.debug("Outbox: {} eventos entregados al broker.", delivered);
        }
    }
}
