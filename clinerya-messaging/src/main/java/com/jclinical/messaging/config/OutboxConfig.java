package com.jclinical.messaging.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jclinical.core.events.DomainEventPublisherPort;
import com.jclinical.messaging.outbox.JdbcOutboxStore;
import com.jclinical.messaging.outbox.OutboxDomainEventPublisher;
import com.jclinical.messaging.outbox.OutboxEventSender;
import com.jclinical.messaging.outbox.OutboxRelay;
import com.jclinical.messaging.outbox.OutboxRelayScheduler;
import com.jclinical.messaging.outbox.OutboxStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;

@Configuration
public class OutboxConfig {

    @Bean
    public OutboxStore outboxStore(JdbcTemplate jdbcTemplate) {
        return new JdbcOutboxStore(jdbcTemplate);
    }

    @Bean
    public DomainEventPublisherPort domainEventPublisher(OutboxStore outboxStore, ObjectMapper objectMapper) {
        return new OutboxDomainEventPublisher(outboxStore, objectMapper, Clock.systemDefaultZone());
    }

    @Bean
    public OutboxRelay outboxRelay(OutboxStore outboxStore, OutboxEventSender sender, ObjectMapper objectMapper) {
        return new OutboxRelay(outboxStore, sender, objectMapper);
    }

    @Bean
    public OutboxRelayScheduler outboxRelayScheduler(OutboxRelay outboxRelay) {
        return new OutboxRelayScheduler(outboxRelay);
    }
}
