package com.jclinical.automation.infra.adapters.out.persistence;

import com.jclinical.automation.domain.ports.out.ChannelSettingsAuditPort;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

public class JdbcChannelSettingsAudit implements ChannelSettingsAuditPort {

    private static final String INSERT_SQL = """
            INSERT INTO automation.channel_settings_audit (id, clinic_id, user_id, action, occurred_at)
            VALUES (?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcChannelSettingsAudit(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void record(UUID clinicId, UUID userId, String action, LocalDateTime at) {
        jdbcTemplate.update(INSERT_SQL, UUID.randomUUID(), clinicId, userId, action, Timestamp.valueOf(at));
    }
}
