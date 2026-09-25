package com.jclinical.messaging.outbox;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Doble de prueba del outbox: conserva el orden de llegada y registra que paso con cada evento.
 */
class InMemoryOutboxStore implements OutboxStore {

    final List<OutboxEvent> pending = new ArrayList<>();
    final List<UUID> published = new ArrayList<>();
    final Map<UUID, String> failed = new LinkedHashMap<>();
    final Map<UUID, String> dead = new LinkedHashMap<>();

    @Override
    public void append(OutboxEvent event) {
        pending.add(event);
    }

    @Override
    public List<OutboxEvent> claimBatch(int limit) {
        return pending.stream()
                .filter(event -> !published.contains(event.id()) && !dead.containsKey(event.id()))
                .limit(limit)
                .toList();
    }

    @Override
    public void markPublished(UUID eventId) {
        published.add(eventId);
    }

    @Override
    public void markFailed(UUID eventId, String error) {
        failed.put(eventId, error);
    }

    @Override
    public void markDead(UUID eventId, String error) {
        dead.put(eventId, error);
    }
}
