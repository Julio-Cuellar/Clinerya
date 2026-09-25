package com.jclinical.automation.infra.adapters.out.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jclinical.automation.domain.model.OutboundReply;
import com.jclinical.automation.domain.model.QueuedMessage;
import com.jclinical.automation.domain.model.QueuedMessage.Audience;
import com.jclinical.automation.domain.ports.out.OutboundDispatchRepositoryPort;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Lado de envio de la cola. Toma el pendiente mas antiguo que ya toca enviar, siempre que no haya uno
 * anterior pendiente para el mismo celular (el orden de la conversacion se respeta aunque haya
 * reintentos), y lo bloquea con SKIP LOCKED para que dos despachadores nunca envien el mismo.
 */
public class JdbcOutboundDispatchRepository implements OutboundDispatchRepositoryPort {

    private static final TypeReference<List<String>> PARAMETERS = new TypeReference<>() {};

    private static final String NEXT_DUE_SQL = """
            SELECT o.id, o.clinic_id, o.phone, o.audience, o.body, o.options, o.template_parameters,
                   o.attempts, o.created_at
              FROM automation.outbound_messages o
             WHERE o.status = 'PENDING'
               AND (o.next_attempt_at IS NULL OR o.next_attempt_at <= ?)
               AND NOT EXISTS (
                   SELECT 1 FROM automation.outbound_messages earlier
                    WHERE earlier.clinic_id = o.clinic_id AND earlier.phone = o.phone
                      AND earlier.status = 'PENDING' AND earlier.created_at < o.created_at)
             ORDER BY o.created_at
             LIMIT 1
             FOR UPDATE SKIP LOCKED
            """;

    private static final String MARK_SENT_SQL = """
            UPDATE automation.outbound_messages
               SET status = 'SENT', wa_message_id = ?, via_template = ?, sent_at = ?, attempts = attempts + 1,
                   last_error = NULL
             WHERE id = ?
            """;

    private static final String SCHEDULE_RETRY_SQL = """
            UPDATE automation.outbound_messages SET attempts = ?, next_attempt_at = ?, last_error = ? WHERE id = ?
            """;

    private static final String MARK_FAILED_SQL = """
            UPDATE automation.outbound_messages SET status = 'FAILED', attempts = ?, last_error = ?, failed_at = ? WHERE id = ?
            """;

    private final JdbcTemplate jdbcTemplate;
    private final ConversationOptionsCodec optionsCodec;
    private final ObjectMapper objectMapper;

    public JdbcOutboundDispatchRepository(JdbcTemplate jdbcTemplate, ConversationOptionsCodec optionsCodec,
                                          ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.optionsCodec = optionsCodec;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<QueuedMessage> lockNextDue(LocalDateTime now) {
        return jdbcTemplate.query(NEXT_DUE_SQL, (row, rowNum) -> toMessage(row), Timestamp.valueOf(now)).stream().findFirst();
    }

    @Override
    public void markSent(UUID id, String waMessageId, boolean viaTemplate, LocalDateTime at) {
        jdbcTemplate.update(MARK_SENT_SQL, waMessageId, viaTemplate, Timestamp.valueOf(at), id);
    }

    @Override
    public void scheduleRetry(UUID id, int attempts, LocalDateTime nextAttemptAt, String error) {
        jdbcTemplate.update(SCHEDULE_RETRY_SQL, attempts, Timestamp.valueOf(nextAttemptAt), error, id);
    }

    @Override
    public void markFailed(UUID id, int attempts, String error, LocalDateTime at) {
        jdbcTemplate.update(MARK_FAILED_SQL, attempts, error, Timestamp.valueOf(at), id);
    }

    private QueuedMessage toMessage(ResultSet row) throws SQLException {
        return new QueuedMessage(
                row.getObject("id", UUID.class),
                row.getObject("clinic_id", UUID.class),
                row.getString("phone"),
                Audience.valueOf(row.getString("audience")),
                new OutboundReply(row.getString("body"), optionsCodec.decode(row.getString("options"))),
                parameters(row.getString("template_parameters")),
                row.getInt("attempts"),
                row.getTimestamp("created_at").toLocalDateTime());
    }

    private List<String> parameters(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, PARAMETERS);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Los parametros de plantilla guardados no son validos.", exception);
        }
    }
}
