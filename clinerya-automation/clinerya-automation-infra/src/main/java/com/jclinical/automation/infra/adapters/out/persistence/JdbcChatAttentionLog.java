package com.jclinical.automation.infra.adapters.out.persistence;

import com.jclinical.automation.domain.model.ChatAttentionEvent;
import com.jclinical.automation.domain.model.ChatAttentionEvent.Action;
import com.jclinical.automation.domain.ports.out.ChatAttentionLogPort;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Bitacora de atencion humana: quien tomo o regreso cada chat y cuando. */
public class JdbcChatAttentionLog implements ChatAttentionLogPort {

    private static final String COLUMNS = "id, clinic_id, phone, action, user_id, at";

    private static final String INSERT_SQL =
            "INSERT INTO automation.chat_attention_log (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?)";

    private static final String LATEST_SQL = "SELECT " + COLUMNS + """
             FROM automation.chat_attention_log
            WHERE clinic_id = ? AND phone = ?
            ORDER BY at DESC
            LIMIT 1
            """;

    /** El ultimo evento de cada chat; los celulares van como parametros (?), nunca concatenados. */
    private static final String LATEST_MANY_SQL = "SELECT DISTINCT ON (phone) " + COLUMNS + """
             FROM automation.chat_attention_log
            WHERE clinic_id = ? AND phone IN (%s)
            ORDER BY phone, at DESC
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcChatAttentionLog(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void record(ChatAttentionEvent event) {
        jdbcTemplate.update(INSERT_SQL, event.id(), event.clinicId(), event.phone(), event.action().name(), event.userId(),
                Timestamp.valueOf(event.at()));
    }

    @Override
    public Optional<ChatAttentionEvent> latest(UUID clinicId, String phone) {
        return jdbcTemplate.query(LATEST_SQL, (row, rowNum) -> toEvent(row), clinicId, phone).stream().findFirst();
    }

    @Override
    public Map<String, ChatAttentionEvent> latest(UUID clinicId, Collection<String> phones) {
        if (phones.isEmpty()) {
            return Map.of();
        }
        String placeholders = String.join(", ", Collections.nCopies(phones.size(), "?"));
        List<Object> arguments = new ArrayList<>();
        arguments.add(clinicId);
        arguments.addAll(phones);
        return jdbcTemplate.query(LATEST_MANY_SQL.formatted(placeholders), (row, rowNum) -> toEvent(row), arguments.toArray())
                .stream()
                .collect(Collectors.toUnmodifiableMap(ChatAttentionEvent::phone, Function.identity()));
    }

    private static ChatAttentionEvent toEvent(ResultSet row) throws SQLException {
        return new ChatAttentionEvent(
                row.getObject("id", UUID.class),
                row.getObject("clinic_id", UUID.class),
                row.getString("phone"),
                Action.valueOf(row.getString("action")),
                row.getObject("user_id", UUID.class),
                row.getTimestamp("at").toLocalDateTime());
    }
}
