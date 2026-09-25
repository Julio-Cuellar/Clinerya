package com.jclinical.automation.infra.adapters.out.persistence;

import com.jclinical.automation.domain.ports.out.CustomerServiceWindowPort;
import com.jclinical.automation.domain.ports.out.InboundMessageLedgerPort;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Registro de ids de WhatsApp recibidos (la llave primaria rechaza repetidos sin carreras) y, con el
 * remitente, la fecha de su ultimo mensaje: eso decide si la ventana de 24 h sigue abierta.
 */
public class JdbcInboundMessageLedger implements InboundMessageLedgerPort, CustomerServiceWindowPort {

    private static final String INSERT_SQL = """
            INSERT INTO automation.inbound_message_ids (wa_message_id, clinic_id, from_phone, received_at)
            VALUES (?, ?, ?, ?)
            ON CONFLICT (wa_message_id) DO NOTHING
            """;

    private static final String LAST_INBOUND_SQL = """
            SELECT max(received_at) FROM automation.inbound_message_ids WHERE clinic_id = ? AND from_phone = ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcInboundMessageLedger(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean recordIfNew(UUID clinicId, String waMessageId, String fromPhone, LocalDateTime receivedAt) {
        return jdbcTemplate.update(INSERT_SQL, waMessageId, clinicId, fromPhone, Timestamp.valueOf(receivedAt)) == 1;
    }

    @Override
    public Optional<LocalDateTime> lastInboundAt(UUID clinicId, String phone) {
        Timestamp last = jdbcTemplate.queryForObject(LAST_INBOUND_SQL, Timestamp.class, clinicId, phone);
        return Optional.ofNullable(last).map(Timestamp::toLocalDateTime);
    }
}
