package com.jclinical.automation.infra.adapters.out.persistence;

import com.jclinical.automation.domain.ports.out.InboundMessageLedgerPort;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

/** Registro de ids de WhatsApp recibidos; la llave primaria rechaza repetidos sin carreras. */
public class JdbcInboundMessageLedger implements InboundMessageLedgerPort {

    private static final String INSERT_SQL = """
            INSERT INTO automation.inbound_message_ids (wa_message_id, clinic_id, received_at)
            VALUES (?, ?, ?)
            ON CONFLICT (wa_message_id) DO NOTHING
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcInboundMessageLedger(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean recordIfNew(UUID clinicId, String waMessageId, LocalDateTime receivedAt) {
        return jdbcTemplate.update(INSERT_SQL, waMessageId, clinicId, Timestamp.valueOf(receivedAt)) == 1;
    }
}
