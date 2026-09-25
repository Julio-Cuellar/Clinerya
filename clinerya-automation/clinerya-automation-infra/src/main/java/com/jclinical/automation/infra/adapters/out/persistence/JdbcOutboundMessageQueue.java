package com.jclinical.automation.infra.adapters.out.persistence;

import com.jclinical.automation.domain.model.PatientNotification;
import com.jclinical.automation.domain.ports.out.OutboundMessageQueuePort;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Cola de mensajes al paciente. Se escribe en la misma transaccion que mueve la conversacion, asi
 * que un mensaje nunca queda sin su cambio de estado (ni al reves). El canal los envia despues.
 */
public class JdbcOutboundMessageQueue implements OutboundMessageQueuePort {

    private static final String INSERT_SQL = """
            INSERT INTO automation.outbound_messages (id, clinic_id, phone, body, options, status, created_at)
            VALUES (?, ?, ?, ?, ?, 'PENDING', ?)
            """;

    private final JdbcTemplate jdbcTemplate;
    private final ConversationOptionsCodec optionsCodec;
    private final Clock clock;

    public JdbcOutboundMessageQueue(JdbcTemplate jdbcTemplate, ConversationOptionsCodec optionsCodec, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.optionsCodec = optionsCodec;
        this.clock = clock;
    }

    @Override
    public void enqueue(PatientNotification notification) {
        jdbcTemplate.update(INSERT_SQL, UUID.randomUUID(), notification.clinicId(), notification.phone(),
                notification.reply().text(), optionsCodec.encode(notification.reply().options()),
                Timestamp.valueOf(LocalDateTime.now(clock)));
    }
}
