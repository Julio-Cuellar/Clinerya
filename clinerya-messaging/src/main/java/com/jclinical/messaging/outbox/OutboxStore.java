package com.jclinical.messaging.outbox;

import java.util.List;
import java.util.UUID;

public interface OutboxStore {

    void append(OutboxEvent event);

    /** Pendientes en orden de publicacion, sin los que ya se entregaron ni los apartados como muertos. */
    List<OutboxEvent> claimBatch(int limit);

    void markPublished(UUID eventId);

    /** El broker rechazo o no respondio: el evento se reintenta en el siguiente ciclo. */
    void markFailed(UUID eventId, String error);

    /** El evento no se puede reconstruir: se aparta para revision y deja de bloquear la cola. */
    void markDead(UUID eventId, String error);
}
