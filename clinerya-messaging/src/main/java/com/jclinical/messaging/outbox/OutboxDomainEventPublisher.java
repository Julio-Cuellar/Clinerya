package com.jclinical.messaging.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jclinical.core.events.DomainEventPublisherPort;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Publica eventos de dominio escribiendolos en el outbox. Como la escritura ocurre en la misma
 * transaccion del cambio de negocio, un rollback descarta tambien el evento y una caida del broker
 * no lo pierde: {@link OutboxRelay} lo entrega despues.
 */
public class OutboxDomainEventPublisher implements DomainEventPublisherPort {

    private final OutboxStore store;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public OutboxDomainEventPublisher(OutboxStore store, ObjectMapper objectMapper, Clock clock) {
        this.store = store;
        // Fechas en ISO-8601 sin importar como venga configurado el mapper: el outbox se lee a mano
        // al diagnosticar, y un arreglo [2026,9,30,16,0] no se entiende de un vistazo.
        this.objectMapper = objectMapper.copy().disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.clock = clock;
    }

    @Override
    public void publish(String routingKey, Object payload) {
        if (routingKey == null || routingKey.isBlank()) {
            throw new IllegalArgumentException("El evento de dominio debe tener una clave de enrutamiento.");
        }
        if (payload == null) {
            throw new IllegalArgumentException("El evento de dominio no puede estar vacio.");
        }
        store.append(new OutboxEvent(
                UUID.randomUUID(),
                routingKey,
                payload.getClass().getName(),
                serialize(payload),
                LocalDateTime.now(clock)));
    }

    private String serialize(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("No se pudo serializar el evento " + payload.getClass().getName() + ".", exception);
        }
    }
}
