package com.jclinical.integrations.infra.config;

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
 * Colas propias de Integraciones para sincronizar citas con Google Calendar por evento de dominio.
 * Cada cola declara su propia dead-letter routing hacia el exchange compartido jclinical.dlx
 * (ver clinerya-messaging), que republica en integrations.dlq tras los reintentos configurados.
 */
@Configuration
public class IntegrationsRabbitConfig {

    public static final String APPOINTMENT_SCHEDULED_QUEUE = "integrations.appointment.scheduled";
    public static final String APPOINTMENT_RESCHEDULED_QUEUE = "integrations.appointment.rescheduled";
    public static final String APPOINTMENT_CANCELLED_QUEUE = "integrations.appointment.cancelled";
    public static final String APPOINTMENT_DELETED_QUEUE = "integrations.appointment.deleted";
    public static final String DEAD_LETTER_QUEUE = "integrations.dlq";

    @Bean
    public Queue appointmentScheduledQueue() {
        return QueueBuilder.durable(APPOINTMENT_SCHEDULED_QUEUE)
                .withArgument("x-dead-letter-exchange", DomainEventRoutingKeys.DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DomainEventRoutingKeys.APPOINTMENT_SCHEDULED)
                .build();
    }

    @Bean
    public Queue appointmentRescheduledQueue() {
        return QueueBuilder.durable(APPOINTMENT_RESCHEDULED_QUEUE)
                .withArgument("x-dead-letter-exchange", DomainEventRoutingKeys.DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DomainEventRoutingKeys.APPOINTMENT_RESCHEDULED)
                .build();
    }

    @Bean
    public Queue appointmentCancelledQueue() {
        return QueueBuilder.durable(APPOINTMENT_CANCELLED_QUEUE)
                .withArgument("x-dead-letter-exchange", DomainEventRoutingKeys.DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DomainEventRoutingKeys.APPOINTMENT_CANCELLED)
                .build();
    }

    @Bean
    public Queue appointmentDeletedQueue() {
        return QueueBuilder.durable(APPOINTMENT_DELETED_QUEUE)
                .withArgument("x-dead-letter-exchange", DomainEventRoutingKeys.DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DomainEventRoutingKeys.APPOINTMENT_DELETED)
                .build();
    }

    @Bean
    public Queue integrationsDeadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    @Bean
    public Binding appointmentScheduledBinding(Queue appointmentScheduledQueue, TopicExchange domainEventsExchange) {
        return BindingBuilder.bind(appointmentScheduledQueue)
                .to(domainEventsExchange)
                .with(DomainEventRoutingKeys.APPOINTMENT_SCHEDULED);
    }

    @Bean
    public Binding appointmentRescheduledBinding(Queue appointmentRescheduledQueue, TopicExchange domainEventsExchange) {
        return BindingBuilder.bind(appointmentRescheduledQueue)
                .to(domainEventsExchange)
                .with(DomainEventRoutingKeys.APPOINTMENT_RESCHEDULED);
    }

    @Bean
    public Binding appointmentCancelledBinding(Queue appointmentCancelledQueue, TopicExchange domainEventsExchange) {
        return BindingBuilder.bind(appointmentCancelledQueue)
                .to(domainEventsExchange)
                .with(DomainEventRoutingKeys.APPOINTMENT_CANCELLED);
    }

    @Bean
    public Binding appointmentDeletedBinding(Queue appointmentDeletedQueue, TopicExchange domainEventsExchange) {
        return BindingBuilder.bind(appointmentDeletedQueue)
                .to(domainEventsExchange)
                .with(DomainEventRoutingKeys.APPOINTMENT_DELETED);
    }

    @Bean
    public Binding integrationsDeadLetterScheduledBinding(Queue integrationsDeadLetterQueue, DirectExchange domainEventsDeadLetterExchange) {
        return BindingBuilder.bind(integrationsDeadLetterQueue)
                .to(domainEventsDeadLetterExchange)
                .with(DomainEventRoutingKeys.APPOINTMENT_SCHEDULED);
    }

    @Bean
    public Binding integrationsDeadLetterRescheduledBinding(Queue integrationsDeadLetterQueue, DirectExchange domainEventsDeadLetterExchange) {
        return BindingBuilder.bind(integrationsDeadLetterQueue)
                .to(domainEventsDeadLetterExchange)
                .with(DomainEventRoutingKeys.APPOINTMENT_RESCHEDULED);
    }

    @Bean
    public Binding integrationsDeadLetterCancelledBinding(Queue integrationsDeadLetterQueue, DirectExchange domainEventsDeadLetterExchange) {
        return BindingBuilder.bind(integrationsDeadLetterQueue)
                .to(domainEventsDeadLetterExchange)
                .with(DomainEventRoutingKeys.APPOINTMENT_CANCELLED);
    }

    @Bean
    public Binding integrationsDeadLetterDeletedBinding(Queue integrationsDeadLetterQueue, DirectExchange domainEventsDeadLetterExchange) {
        return BindingBuilder.bind(integrationsDeadLetterQueue)
                .to(domainEventsDeadLetterExchange)
                .with(DomainEventRoutingKeys.APPOINTMENT_DELETED);
    }
}
