package com.jclinical.messaging.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

/**
 * Outbox en PostgreSQL. {@link #claimBatch} bloquea las filas con SKIP LOCKED, asi que debe
 * llamarse dentro de una transaccion: dos instancias de la aplicacion nunca entregan el mismo
 * evento a la vez.
 */
@RequiredArgsConstructor
public class JdbcOutboxStore implements OutboxStore {

    private static final String INSERT_SQL = """
            INSERT INTO messaging.outbox_events (id, routing_key, payload_type, payload, created_at)
            VALUES (?, ?, ?, ?, ?)
            """;

    private static final String CLAIM_SQL = """
            SELECT id, routing_key, payload_type, payload, created_at
              FROM messaging.outbox_events
             WHERE dead_at IS NULL
             ORDER BY seq
             FOR UPDATE SKIP LOCKED
             LIMIT ?
            """;

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void append(OutboxEvent event) {
        jdbcTemplate.update(INSERT_SQL, event.id(), event.routingKey(), event.payloadType(), event.payload(),
                Timestamp.valueOf(event.createdAt()));
    }

    @Override
    public List<OutboxEvent> claimBatch(int limit) {
        return jdbcTemplate.query(CLAIM_SQL, (row, rowNum) -> new OutboxEvent(
                row.getObject("id", UUID.class),
                row.getString("routing_key"),
                row.getString("payload_type"),
                row.getString("payload"),
                row.getTimestamp("created_at").toLocalDateTime()), limit);
    }

    @Override
    public void markPublished(UUID eventId) {
        jdbcTemplate.update("DELETE FROM messaging.outbox_events WHERE id = ?", eventId);
    }

    @Override
    public void markFailed(UUID eventId, String error) {
        jdbcTemplate.update("UPDATE messaging.outbox_events SET attempts = attempts + 1, last_error = ? WHERE id = ?",
                error, eventId);
    }

    @Override
    public void markDead(UUID eventId, String error) {
        jdbcTemplate.update(
                "UPDATE messaging.outbox_events SET dead_at = CURRENT_TIMESTAMP, last_error = ? WHERE id = ?",
                error, eventId);
    }
}
