package com.jclinical.messaging.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.jclinical.core.events.AppointmentCancelledEvent;
import com.jclinical.core.events.AppointmentScheduledEvent;
import com.jclinical.core.events.DomainEventRoutingKeys;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El relay saca los eventos del outbox y los entrega al broker. Garantias:
 * se entregan en orden, con su tipo original, y un broker caido no se salta eventos.
 */
class OutboxRelayTest {

    private final InMemoryOutboxStore store = new InMemoryOutboxStore();
    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
    private final OutboxDomainEventPublisher publisher =
            new OutboxDomainEventPublisher(store, objectMapper, Clock.systemUTC());
    private final RecordingSender sender = new RecordingSender();
    private final OutboxRelay relay = new OutboxRelay(store, sender, objectMapper);

    @Test
    void deliversTheEventWithItsOriginalTypeAndRoutingKey() {
        AppointmentScheduledEvent event = OutboxDomainEventPublisherTest.scheduledEvent();
        publisher.publish(DomainEventRoutingKeys.APPOINTMENT_SCHEDULED, event);

        int delivered = relay.relay(10);

        assertEquals(1, delivered);
        assertEquals(List.of(DomainEventRoutingKeys.APPOINTMENT_SCHEDULED), sender.routingKeys);
        assertEquals(event, sender.payloads.get(0), "el consumidor recibe el mismo record que se publico");
        assertEquals(List.of(store.pending.get(0).id()), store.published);
    }

    @Test
    void deliversEventsInTheOrderTheyWerePublished() {
        UUID appointmentId = UUID.randomUUID();
        publisher.publish(DomainEventRoutingKeys.APPOINTMENT_SCHEDULED, OutboxDomainEventPublisherTest.scheduledEvent());
        publisher.publish(DomainEventRoutingKeys.APPOINTMENT_CANCELLED,
                new AppointmentCancelledEvent(UUID.randomUUID(), UUID.randomUUID(), appointmentId, LocalDateTime.now(), null, null, null, null));

        relay.relay(10);

        assertEquals(List.of(DomainEventRoutingKeys.APPOINTMENT_SCHEDULED, DomainEventRoutingKeys.APPOINTMENT_CANCELLED),
                sender.routingKeys);
    }

    @Test
    void stopsAtTheFirstBrokerFailureSoNoLaterEventOvertakesIt() {
        publisher.publish(DomainEventRoutingKeys.APPOINTMENT_SCHEDULED, OutboxDomainEventPublisherTest.scheduledEvent());
        publisher.publish(DomainEventRoutingKeys.APPOINTMENT_CANCELLED,
                new AppointmentCancelledEvent(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), LocalDateTime.now(), null, null, null, null));
        sender.failNext = true;

        int delivered = relay.relay(10);

        assertEquals(0, delivered);
        assertTrue(sender.routingKeys.isEmpty(), "tras el fallo no se intenta el siguiente evento");
        UUID first = store.pending.get(0).id();
        assertEquals(List.of(first), List.copyOf(store.failed.keySet()));
        assertTrue(store.published.isEmpty());
        assertTrue(store.dead.isEmpty(), "un broker caido no convierte el evento en muerto");
    }

    @Test
    void anUnreadableEventIsSetAsideAndTheRestKeepFlowing() {
        store.append(new OutboxEvent(UUID.randomUUID(), "legacy.event", "com.jclinical.core.events.NoLongerExists",
                "{}", LocalDateTime.now()));
        publisher.publish(DomainEventRoutingKeys.APPOINTMENT_SCHEDULED, OutboxDomainEventPublisherTest.scheduledEvent());

        int delivered = relay.relay(10);

        assertEquals(1, delivered);
        assertEquals(List.of(store.pending.get(0).id()), List.copyOf(store.dead.keySet()));
        assertEquals(List.of(DomainEventRoutingKeys.APPOINTMENT_SCHEDULED), sender.routingKeys);
    }

    @Test
    void respectsTheBatchSize() {
        for (int i = 0; i < 5; i++) {
            publisher.publish(DomainEventRoutingKeys.APPOINTMENT_SCHEDULED, OutboxDomainEventPublisherTest.scheduledEvent());
        }

        assertEquals(2, relay.relay(2));
        assertEquals(2, sender.routingKeys.size());
    }

    static final class RecordingSender implements OutboxEventSender {
        final List<String> routingKeys = new ArrayList<>();
        final List<Object> payloads = new ArrayList<>();
        boolean failNext;

        @Override
        public void send(String routingKey, Object payload) {
            if (failNext) {
                failNext = false;
                throw new IllegalStateException("broker no disponible");
            }
            routingKeys.add(routingKey);
            payloads.add(payload);
        }
    }
}
