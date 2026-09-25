package com.jclinical.messaging.adapters;

import com.jclinical.core.events.DomainEventRoutingKeys;
import com.jclinical.messaging.outbox.OutboxEventSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Ultimo tramo del outbox: entrega el evento ya reconstruido al exchange de dominio. Se envia el
 * record original con convertAndSend para que los consumidores reciban exactamente el mismo mensaje
 * (JSON y cabecera de tipo) que cuando los servicios publicaban directo.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RabbitOutboxEventSender implements OutboxEventSender {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void send(String routingKey, Object payload) {
        log.info(">>>> [RABBITMQ] Publicando evento '{}' -> {}", routingKey, payload);
        rabbitTemplate.convertAndSend(DomainEventRoutingKeys.DOMAIN_EVENTS_EXCHANGE, routingKey, payload);
    }
}
