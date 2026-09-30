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
    public static final String WHATSAPP_MESSAGE_RECEIVED_QUEUE = "automation.whatsapp.message.received";
    public static final String REMINDER_SCHEDULED_QUEUE = "automation.reminders.appointment.scheduled";
    public static final String REMINDER_RESCHEDULED_QUEUE = "automation.reminders.appointment.rescheduled";
    public static final String REMINDER_CANCELLED_QUEUE = "automation.reminders.appointment.cancelled";
    public static final String REMINDER_DELETED_QUEUE = "automation.reminders.appointment.deleted";

    @Bean
    public Queue automationRequestResolvedQueue() {
        return QueueBuilder.durable(REQUEST_RESOLVED_QUEUE)
                .withArgument("x-dead-letter-exchange", DomainEventRoutingKeys.DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DomainEventRoutingKeys.APPOINTMENT_REQUEST_RESOLVED)
                .build();
    }

    @Bean
    public Queue automationWhatsAppMessageQueue() {
        return QueueBuilder.durable(WHATSAPP_MESSAGE_RECEIVED_QUEUE)
                .withArgument("x-dead-letter-exchange", DomainEventRoutingKeys.DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DomainEventRoutingKeys.WHATSAPP_MESSAGE_RECEIVED)
                .build();
    }

    @Bean
    public Binding automationWhatsAppMessageBinding(Queue automationWhatsAppMessageQueue, TopicExchange domainEventsExchange) {
        return BindingBuilder.bind(automationWhatsAppMessageQueue)
                .to(domainEventsExchange)
                .with(DomainEventRoutingKeys.WHATSAPP_MESSAGE_RECEIVED);
    }

    @Bean
    public Binding automationWhatsAppDeadLetterBinding(Queue automationDeadLetterQueue, DirectExchange domainEventsDeadLetterExchange) {
        return BindingBuilder.bind(automationDeadLetterQueue)
                .to(domainEventsDeadLetterExchange)
                .with(DomainEventRoutingKeys.WHATSAPP_MESSAGE_RECEIVED);
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

    // Recordatorio de cita (plan v2, S6): appointment_scheduled
    @Bean
    public Queue automationReminderScheduledQueue() {
        return QueueBuilder.durable(REMINDER_SCHEDULED_QUEUE)
                .withArgument("x-dead-letter-exchange", DomainEventRoutingKeys.DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DomainEventRoutingKeys.APPOINTMENT_SCHEDULED)
                .build();
    }

    @Bean
    public Binding automationReminderScheduledBinding(Queue automationReminderScheduledQueue, TopicExchange domainEventsExchange) {
        return BindingBuilder.bind(automationReminderScheduledQueue)
                .to(domainEventsExchange)
                .with(DomainEventRoutingKeys.APPOINTMENT_SCHEDULED);
    }

    @Bean
    public Binding automationReminderScheduledDeadLetterBinding(Queue automationDeadLetterQueue,
                                                             DirectExchange domainEventsDeadLetterExchange) {
        return BindingBuilder.bind(automationDeadLetterQueue)
                .to(domainEventsDeadLetterExchange)
                .with(DomainEventRoutingKeys.APPOINTMENT_SCHEDULED);
    }

    // Recordatorio de cita (plan v2, S6): appointment_rescheduled
    @Bean
    public Queue automationReminderRescheduledQueue() {
        return QueueBuilder.durable(REMINDER_RESCHEDULED_QUEUE)
                .withArgument("x-dead-letter-exchange", DomainEventRoutingKeys.DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DomainEventRoutingKeys.APPOINTMENT_RESCHEDULED)
                .build();
    }

    @Bean
    public Binding automationReminderRescheduledBinding(Queue automationReminderRescheduledQueue, TopicExchange domainEventsExchange) {
        return BindingBuilder.bind(automationReminderRescheduledQueue)
                .to(domainEventsExchange)
                .with(DomainEventRoutingKeys.APPOINTMENT_RESCHEDULED);
    }

    @Bean
    public Binding automationReminderRescheduledDeadLetterBinding(Queue automationDeadLetterQueue,
                                                             DirectExchange domainEventsDeadLetterExchange) {
        return BindingBuilder.bind(automationDeadLetterQueue)
                .to(domainEventsDeadLetterExchange)
                .with(DomainEventRoutingKeys.APPOINTMENT_RESCHEDULED);
    }

    // Recordatorio de cita (plan v2, S6): appointment_cancelled
    @Bean
    public Queue automationReminderCancelledQueue() {
        return QueueBuilder.durable(REMINDER_CANCELLED_QUEUE)
                .withArgument("x-dead-letter-exchange", DomainEventRoutingKeys.DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DomainEventRoutingKeys.APPOINTMENT_CANCELLED)
                .build();
    }

    @Bean
    public Binding automationReminderCancelledBinding(Queue automationReminderCancelledQueue, TopicExchange domainEventsExchange) {
        return BindingBuilder.bind(automationReminderCancelledQueue)
                .to(domainEventsExchange)
                .with(DomainEventRoutingKeys.APPOINTMENT_CANCELLED);
    }

    @Bean
    public Binding automationReminderCancelledDeadLetterBinding(Queue automationDeadLetterQueue,
                                                             DirectExchange domainEventsDeadLetterExchange) {
        return BindingBuilder.bind(automationDeadLetterQueue)
                .to(domainEventsDeadLetterExchange)
                .with(DomainEventRoutingKeys.APPOINTMENT_CANCELLED);
    }

    // Recordatorio de cita (plan v2, S6): appointment_deleted
    @Bean
    public Queue automationReminderDeletedQueue() {
        return QueueBuilder.durable(REMINDER_DELETED_QUEUE)
                .withArgument("x-dead-letter-exchange", DomainEventRoutingKeys.DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DomainEventRoutingKeys.APPOINTMENT_DELETED)
                .build();
    }

    @Bean
    public Binding automationReminderDeletedBinding(Queue automationReminderDeletedQueue, TopicExchange domainEventsExchange) {
        return BindingBuilder.bind(automationReminderDeletedQueue)
                .to(domainEventsExchange)
                .with(DomainEventRoutingKeys.APPOINTMENT_DELETED);
    }

    @Bean
    public Binding automationReminderDeletedDeadLetterBinding(Queue automationDeadLetterQueue,
                                                             DirectExchange domainEventsDeadLetterExchange) {
        return BindingBuilder.bind(automationDeadLetterQueue)
                .to(domainEventsDeadLetterExchange)
                .with(DomainEventRoutingKeys.APPOINTMENT_DELETED);
    }
}
