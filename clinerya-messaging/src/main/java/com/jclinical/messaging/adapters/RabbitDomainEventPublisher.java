package com.jclinical.messaging.adapters;

import com.jclinical.core.events.DomainEventPublisherPort;
import com.jclinical.core.events.DomainEventRoutingKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RabbitDomainEventPublisher implements DomainEventPublisherPort {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publish(String routingKey, Object payload) {
        log.info(">>>> [RABBITMQ] Publicando evento '{}' -> {}", routingKey, payload);
        rabbitTemplate.convertAndSend(DomainEventRoutingKeys.DOMAIN_EVENTS_EXCHANGE, routingKey, payload);
    }
}
