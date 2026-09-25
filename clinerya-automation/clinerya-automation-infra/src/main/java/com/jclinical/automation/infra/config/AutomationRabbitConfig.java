package com.jclinical.automation.infra.config;

import com.jclinical.core.events.DomainEventRoutingKeys;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cola de la automatizacion para la respuesta del medico. Tras los reintentos del contenedor el
 * mensaje pasa al exchange de dead letters compartido y queda en automation.dlq.
 */
@Configuration
public class AutomationRabbitConfig {

    public static final String REQUEST_RESOLVED_QUEUE = "automation.appointment-request.resolved";
    public static final String DEAD_LETTER_QUEUE = "automation.dlq";

    @Bean
    public Queue automationRequestResolvedQueue() {
        return QueueBuilder.durable(REQUEST_RESOLVED_QUEUE)
                .withArgument("x-dead-letter-exchange", DomainEventRoutingKeys.DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DomainEventRoutingKeys.APPOINTMENT_REQUEST_RESOLVED)
                .build();
    }

    @Bean
    public Queue automationDeadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    @Bean
    public Binding automationRequestResolvedBinding(Queue automationRequestResolvedQueue, TopicExchange domainEventsExchange) {
        return BindingBuilder.bind(automationRequestResolvedQueue)
                .to(domainEventsExchange)
                .with(DomainEventRoutingKeys.APPOINTMENT_REQUEST_RESOLVED);
    }

    @Bean
    public Binding automationDeadLetterBinding(Queue automationDeadLetterQueue, DirectExchange domainEventsDeadLetterExchange) {
        return BindingBuilder.bind(automationDeadLetterQueue)
                .to(domainEventsDeadLetterExchange)
                .with(DomainEventRoutingKeys.APPOINTMENT_REQUEST_RESOLVED);
    }
}
