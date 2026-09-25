package com.jclinical.messaging.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

/**
 * Entrega al broker los eventos pendientes del outbox, en el orden en que se publicaron.
 *
 * <p>Ante un fallo del broker se detiene: seguir con el siguiente evento permitiria que, por
 * ejemplo, una cancelacion llegara antes que el agendado de la misma cita. Un evento que no se
 * puede reconstruir (su clase ya no existe o el JSON no corresponde) se aparta como muerto para
 * que no bloquee la cola para siempre.
 *
 * <p>La entrega es al-menos-una-vez: si el broker acepta el mensaje pero la transaccion que lo
 * marca como publicado no confirma, se volvera a enviar. Los consumidores deben ser idempotentes.
 */
@Slf4j
public class OutboxRelay {

    private final OutboxStore store;
    private final OutboxEventSender sender;
    private final ObjectMapper objectMapper;

    public OutboxRelay(OutboxStore store, OutboxEventSender sender, ObjectMapper objectMapper) {
        this.store = store;
        this.sender = sender;
        this.objectMapper = objectMapper;
    }

    /** @return cuantos eventos se entregaron en este ciclo. */
    public int relay(int batchSize) {
        int delivered = 0;
        for (OutboxEvent event : store.claimBatch(batchSize)) {
            Object payload;
            try {
                payload = objectMapper.readValue(event.payload(), Class.forName(event.payloadType()));
            } catch (ClassNotFoundException | java.io.IOException exception) {
                log.error("Evento {} ({}) apartado: no se puede reconstruir como {}.",
                        event.id(), event.routingKey(), event.payloadType(), exception);
                store.markDead(event.id(), exception.getMessage());
                continue;
            }
            try {
                sender.send(event.routingKey(), payload);
            } catch (RuntimeException exception) {
                log.warn("El broker no acepto el evento {} ({}); se reintenta en el siguiente ciclo.",
                        event.id(), event.routingKey(), exception);
                store.markFailed(event.id(), exception.getMessage());
                break;
            }
            store.markPublished(event.id());
            delivered++;
        }
        return delivered;
    }
}
