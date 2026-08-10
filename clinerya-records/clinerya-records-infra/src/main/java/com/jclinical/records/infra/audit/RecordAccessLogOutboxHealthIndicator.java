package com.jclinical.records.infra.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component("recordAccessAuditOutbox")
@RequiredArgsConstructor
public class RecordAccessLogOutboxHealthIndicator implements HealthIndicator {

    private static final String STATUS_SQL = """
            SELECT count(*) AS pending,
                   COALESCE(EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - min(enqueued_at))), 0) AS oldest_age_seconds
              FROM records.record_access_log_outbox
            """;

    private final JdbcTemplate jdbcTemplate;

    @Value("${app.audit.outbox.health.max-pending:5000}")
    private long maxPending;

    @Value("${app.audit.outbox.health.max-oldest-age-seconds:300}")
    private long maxOldestAgeSeconds;

    @Override
    public Health health() {
        try {
            Map<String, Object> status = jdbcTemplate.queryForMap(STATUS_SQL);
            long pending = ((Number) status.get("pending")).longValue();
            long oldestAgeSeconds = ((Number) status.get("oldest_age_seconds")).longValue();
            Health.Builder builder = pending > maxPending || oldestAgeSeconds > maxOldestAgeSeconds
                    ? Health.status("DEGRADED")
                    : Health.up();
            return builder
                    .withDetail("pending", pending)
                    .withDetail("oldestAgeSeconds", oldestAgeSeconds)
                    .withDetail("maxPending", maxPending)
                    .withDetail("maxOldestAgeSeconds", maxOldestAgeSeconds)
                    .build();
        } catch (RuntimeException exception) {
            return Health.down(exception).build();
        }
    }
}
