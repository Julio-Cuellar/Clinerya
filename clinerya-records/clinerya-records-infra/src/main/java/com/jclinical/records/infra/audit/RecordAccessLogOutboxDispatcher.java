package com.jclinical.records.infra.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class RecordAccessLogOutboxDispatcher {

    private static final String DISPATCH_SQL = """
            WITH claimed AS MATERIALIZED (
                SELECT id, clinic_id, patient_id, user_id, user_name, resource_type,
                       resource_id, action_type, ip_address, user_agent, created_at
                  FROM records.record_access_log_outbox
                 ORDER BY enqueued_at, id
                 FOR UPDATE SKIP LOCKED
                 LIMIT ?
            ), inserted AS (
                INSERT INTO records.record_access_logs (
                    id, clinic_id, patient_id, user_id, user_name, resource_type,
                    resource_id, action_type, ip_address, user_agent, created_at
                )
                SELECT id, clinic_id, patient_id, user_id, user_name, resource_type,
                       resource_id, action_type, ip_address, user_agent, created_at
                  FROM claimed
                ON CONFLICT (id) DO NOTHING
                RETURNING id
            ), deleted AS (
                DELETE FROM records.record_access_log_outbox pending
                 USING claimed
                 WHERE pending.id = claimed.id
                 RETURNING pending.id
            )
            SELECT count(*) FROM deleted
            """;

    private static final String PENDING_COUNT_SQL = "SELECT count(*) FROM records.record_access_log_outbox";

    private final JdbcTemplate jdbcTemplate;
    private final RecordAccessLogMetrics metrics;

    @Value("${app.audit.outbox.batch-size:200}")
    private int batchSize;

    @Scheduled(
            fixedDelayString = "${app.audit.outbox.dispatch-delay-ms:500}",
            initialDelayString = "${app.audit.outbox.initial-delay-ms:1000}")
    @Transactional
    public void dispatchPending() {
        Instant startedAt = Instant.now();
        try {
            Long dispatched = jdbcTemplate.queryForObject(DISPATCH_SQL, Long.class, batchSize);
            metrics.recordDispatch(dispatched == null ? 0 : dispatched.intValue(), Duration.between(startedAt, Instant.now()));
        } catch (RuntimeException exception) {
            metrics.recordDispatchFailure(Duration.between(startedAt, Instant.now()));
            log.error("No se pudo despachar la bitacora de accesos desde el outbox.", exception);
            throw exception;
        }
    }

    @Scheduled(
            fixedDelayString = "${app.audit.outbox.metrics-refresh-delay-ms:10000}",
            initialDelayString = "${app.audit.outbox.initial-delay-ms:1000}")
    public void refreshPendingMetric() {
        try {
            metrics.updatePending(pendingCount());
        } catch (RuntimeException exception) {
            log.warn("No se pudo actualizar la metrica de eventos pendientes de auditoria.", exception);
        }
    }

    private long pendingCount() {
        Long count = jdbcTemplate.queryForObject(PENDING_COUNT_SQL, Long.class);
        return count == null ? 0 : count;
    }
}
