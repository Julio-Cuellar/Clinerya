package com.jclinical.records.infra.audit;

import com.jclinical.records.domain.model.RecordAccessLog;
import com.jclinical.records.domain.ports.out.RecordAccessLogOutboxPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;

@Repository
@RequiredArgsConstructor
public class SqlRecordAccessLogOutbox implements RecordAccessLogOutboxPort {

    private static final String INSERT_SQL = """
            INSERT INTO records.record_access_log_outbox (
                id, clinic_id, patient_id, user_id, user_name, resource_type,
                resource_id, action_type, ip_address, user_agent, created_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (id) DO NOTHING
            """;

    private final JdbcTemplate jdbcTemplate;
    private final RecordAccessLogMetrics metrics;

    @Override
    public void enqueue(RecordAccessLog log) {
        int inserted = jdbcTemplate.update(
                INSERT_SQL,
                log.getId(),
                log.getClinicId(),
                log.getPatientId(),
                log.getUserId(),
                log.getUserName(),
                log.getResourceType(),
                log.getResourceId(),
                log.getActionType(),
                log.getIpAddress(),
                log.getUserAgent(),
                Timestamp.valueOf(log.getCreatedAt()));
        if (inserted == 1) {
            metrics.recordEnqueued();
        }
    }
}
