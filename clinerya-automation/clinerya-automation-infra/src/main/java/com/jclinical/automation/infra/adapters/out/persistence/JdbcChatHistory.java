package com.jclinical.automation.infra.adapters.out.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jclinical.automation.domain.model.ChatMessage;
import com.jclinical.automation.domain.model.ChatSummary;
import com.jclinical.automation.domain.ports.out.ChatHistoryPort;
import com.jclinical.core.security.crypto.FieldCipher;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Hilos de WhatsApp por clinica y celular. El texto y las opciones pueden traer datos de salud o
 * nombres, asi que se guardan cifrados (FieldCipher); el celular y la fecha quedan en claro para
 * poder agrupar, paginar y purgar.
 */
public class JdbcChatHistory implements ChatHistoryPort {

    private static final TypeReference<List<String>> LABELS = new TypeReference<>() {};

    private static final String INSERT_SQL = """
            INSERT INTO automation.chat_messages (id, clinic_id, phone, direction, body, option_labels, created_at,
                author_user_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String CHATS_SQL = """
            SELECT phone, max(created_at) AS last_message_at, count(*) AS message_count
              FROM automation.chat_messages
             WHERE clinic_id = ?
             GROUP BY phone
             ORDER BY last_message_at DESC
             LIMIT ?
            """;

    private static final String COLUMNS = "id, clinic_id, phone, direction, body, option_labels, created_at, author_user_id";

    private static final String LATEST_SQL = "SELECT " + COLUMNS + """
             FROM automation.chat_messages
            WHERE clinic_id = ? AND phone = ?
            ORDER BY created_at DESC
            LIMIT ?
            """;

    private static final String BEFORE_SQL = "SELECT " + COLUMNS + """
             FROM automation.chat_messages
            WHERE clinic_id = ? AND phone = ? AND created_at < ?
            ORDER BY created_at DESC
            LIMIT ?
            """;

    private static final String AFTER_SQL = "SELECT " + COLUMNS + """
             FROM automation.chat_messages
            WHERE clinic_id = ? AND phone = ? AND created_at > ?
            ORDER BY created_at
            LIMIT ?
            """;

    private static final String LAST_INBOUND_SQL = """
            SELECT max(created_at) FROM automation.chat_messages
             WHERE clinic_id = ? AND phone = ? AND direction = 'INBOUND'
            """;

    private static final String DELETE_SQL = "DELETE FROM automation.chat_messages WHERE clinic_id = ? AND created_at < ?";

    private final JdbcTemplate jdbcTemplate;
    private final FieldCipher cipher;
    private final ObjectMapper objectMapper;

    public JdbcChatHistory(JdbcTemplate jdbcTemplate, FieldCipher cipher, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.cipher = cipher;
        this.objectMapper = objectMapper;
    }

    @Override
    public void record(ChatMessage message) {
        jdbcTemplate.update(INSERT_SQL, message.id(), message.clinicId(), message.phone(), message.direction().name(),
                cipher.encrypt(message.text() == null ? "" : message.text()),
                cipher.encrypt(labelsJson(message.optionLabels())), Timestamp.valueOf(message.at()), message.authorUserId());
    }

    @Override
    public List<ChatSummary> findChats(UUID clinicId, int limit) {
        return jdbcTemplate.query(CHATS_SQL, (row, rowNum) -> new ChatSummary(row.getString("phone"), List.of(),
                row.getTimestamp("last_message_at").toLocalDateTime(), row.getInt("message_count")), clinicId, limit);
    }

    @Override
    public List<ChatMessage> findMessages(UUID clinicId, String phone, LocalDateTime before, int limit) {
        if (before == null) {
            return jdbcTemplate.query(LATEST_SQL, (row, rowNum) -> toMessage(row), clinicId, phone, limit);
        }
        return jdbcTemplate.query(BEFORE_SQL, (row, rowNum) -> toMessage(row), clinicId, phone, Timestamp.valueOf(before), limit);
    }

    @Override
    public List<ChatMessage> findMessagesAfter(UUID clinicId, String phone, LocalDateTime after, int limit) {
        return jdbcTemplate.query(AFTER_SQL, (row, rowNum) -> toMessage(row), clinicId, phone, Timestamp.valueOf(after), limit);
    }

    @Override
    public Optional<LocalDateTime> lastInboundAt(UUID clinicId, String phone) {
        Timestamp last = jdbcTemplate.queryForObject(LAST_INBOUND_SQL, Timestamp.class, clinicId, phone);
        return Optional.ofNullable(last).map(Timestamp::toLocalDateTime);
    }

    @Override
    public int deleteOlderThan(UUID clinicId, LocalDateTime cutoff) {
        return jdbcTemplate.update(DELETE_SQL, clinicId, Timestamp.valueOf(cutoff));
    }

    private ChatMessage toMessage(ResultSet row) throws SQLException {
        return new ChatMessage(
                row.getObject("id", UUID.class),
                row.getObject("clinic_id", UUID.class),
                row.getString("phone"),
                ChatMessage.Direction.valueOf(row.getString("direction")),
                cipher.decrypt(row.getString("body")),
                labels(cipher.decrypt(row.getString("option_labels"))),
                row.getTimestamp("created_at").toLocalDateTime(),
                row.getObject("author_user_id", UUID.class));
    }

    private String labelsJson(List<String> labels) {
        try {
            return objectMapper.writeValueAsString(labels);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("No se pudieron guardar las opciones del mensaje.", exception);
        }
    }

    private List<String> labels(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, LABELS);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Las opciones guardadas del mensaje no son validas.", exception);
        }
    }
}
