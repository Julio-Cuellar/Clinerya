package com.jclinical.messaging.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.jclinical.core.events.AppointmentScheduledEvent;
import com.jclinical.core.events.DomainEventRoutingKeys;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El publicador ya no habla con RabbitMQ: deja el evento en el outbox, dentro de la misma
 * transaccion del cambio de negocio, para que un rollback nunca produzca un evento fantasma
 * ni una caida del broker pierda uno real.
 */
class OutboxDomainEventPublisherTest {

    private static final Instant NOW = Instant.parse("2026-09-24T15:30:00Z");

    private final InMemoryOutboxStore store = new InMemoryOutboxStore();
    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
    private final OutboxDomainEventPublisher publisher =
            new OutboxDomainEventPublisher(store, objectMapper, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void storesTheEventInTheOutboxInsteadOfSendingIt() {
        AppointmentScheduledEvent event = scheduledEvent();

        publisher.publish(DomainEventRoutingKeys.APPOINTMENT_SCHEDULED, event);

        assertEquals(1, store.pending.size());
        OutboxEvent stored = store.pending.get(0);
        assertNotNull(stored.id());
        assertEquals(DomainEventRoutingKeys.APPOINTMENT_SCHEDULED, stored.routingKey());
        assertEquals(AppointmentScheduledEvent.class.getName(), stored.payloadType());
        assertEquals(LocalDateTime.ofInstant(NOW, ZoneOffset.UTC), stored.createdAt());
    }

    @Test
    void serializesThePayloadAsJson() {
        AppointmentScheduledEvent event = scheduledEvent();

        publisher.publish(DomainEventRoutingKeys.APPOINTMENT_SCHEDULED, event);

        String payload = store.pending.get(0).payload();
        assertTrue(payload.contains(event.appointmentId().toString()), payload);
        assertTrue(payload.contains("2026-09-30T16:00"), "las fechas viajan en ISO-8601: " + payload);
    }

    @Test
    void rejectsAnEventWithoutRoutingKey() {
        assertThrows(IllegalArgumentException.class, () -> publisher.publish(" ", scheduledEvent()));
        assertTrue(store.pending.isEmpty());
    }

    @Test
    void rejectsAnEmptyPayload() {
        assertThrows(IllegalArgumentException.class,
                () -> publisher.publish(DomainEventRoutingKeys.APPOINTMENT_SCHEDULED, null));
        assertTrue(store.pending.isEmpty());
    }

    static AppointmentScheduledEvent scheduledEvent() {
        return new AppointmentScheduledEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                LocalDateTime.of(2026, 9, 30, 16, 0), LocalDateTime.of(2026, 9, 30, 16, 45),
                LocalDateTime.of(2026, 9, 24, 9, 30));
    }
}
