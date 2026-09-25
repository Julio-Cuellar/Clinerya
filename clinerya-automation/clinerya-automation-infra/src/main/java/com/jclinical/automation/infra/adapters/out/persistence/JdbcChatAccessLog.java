package com.jclinical.automation.infra.adapters.out.persistence;

import com.jclinical.automation.domain.model.ChatAccess;
import com.jclinical.automation.domain.ports.out.ChatAccessLogPort;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Bitacora de lecturas de chats: quien leyo que hilo y cuando. Solo se agrega, nunca se edita. */
public class JdbcChatAccessLog implements ChatAccessLogPort {

    private static final String INSERT_SQL = """
            INSERT INTO automation.chat_access_log (id, clinic_id, phone, user_id, accessed_at) VALUES (?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcChatAccessLog(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void record(ChatAccess access) {
        jdbcTemplate.update(INSERT_SQL, access.id(), access.clinicId(), access.phone(), access.userId(),
                Timestamp.valueOf(access.accessedAt()));
    }

    @Override
    public List<ChatAccess> find(UUID clinicId, String phone, UUID userId, LocalDateTime from, LocalDateTime to, int limit) {
        StringBuilder sql = new StringBuilder(
                "SELECT id, clinic_id, phone, user_id, accessed_at FROM automation.chat_access_log WHERE clinic_id = ?");
        List<Object> params = new ArrayList<>(List.of(clinicId));
        if (phone != null) {
            sql.append(" AND phone = ?");
            params.add(phone);
        }
        if (userId != null) {
            sql.append(" AND user_id = ?");
            params.add(userId);
        }
        if (from != null) {
            sql.append(" AND accessed_at >= ?");
            params.add(Timestamp.valueOf(from));
        }
        if (to != null) {
            sql.append(" AND accessed_at < ?");
            params.add(Timestamp.valueOf(to));
        }
        sql.append(" ORDER BY accessed_at DESC LIMIT ?");
        params.add(limit);
        return jdbcTemplate.query(sql.toString(), (row, rowNum) -> new ChatAccess(
                row.getObject("id", UUID.class),
                row.getObject("clinic_id", UUID.class),
                row.getString("phone"),
                row.getObject("user_id", UUID.class),
                row.getTimestamp("accessed_at").toLocalDateTime()), params.toArray());
    }
}
