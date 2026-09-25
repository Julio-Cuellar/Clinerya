package com.jclinical.automation.infra.adapters.out.persistence;

import com.jclinical.automation.domain.model.Conversation;
import com.jclinical.automation.domain.model.ConversationState;
import com.jclinical.automation.domain.ports.out.ConversationRepositoryPort;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class JdbcConversationRepository implements ConversationRepositoryPort {

    private static final String TERMINAL_STATES = Arrays.stream(ConversationState.values())
            .filter(ConversationState::isTerminal)
            .map(state -> "'" + state.name() + "'")
            .collect(Collectors.joining(", "));

    private static final String FIND_ACTIVE_SQL = """
            SELECT id, clinic_id, phone, state, patient_id, patient_name, doctor_staff_id, doctor_name,
                   request_id, offered_options, unrecognized_count, created_at, last_activity_at
              FROM automation.conversations
             WHERE clinic_id = ? AND phone = ? AND state NOT IN (%s)
             ORDER BY created_at DESC
             LIMIT 1
            """.formatted(TERMINAL_STATES);

    private static final String FIND_BY_ID_SQL = """
            SELECT id, clinic_id, phone, state, patient_id, patient_name, doctor_staff_id, doctor_name,
                   request_id, offered_options, unrecognized_count, created_at, last_activity_at
              FROM automation.conversations
             WHERE id = ?
            """;

    private static final String UPSERT_SQL = """
            INSERT INTO automation.conversations (
                id, clinic_id, phone, state, patient_id, patient_name, doctor_staff_id, doctor_name,
                request_id, offered_options, unrecognized_count, created_at, last_activity_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET
                state = EXCLUDED.state,
                patient_id = EXCLUDED.patient_id,
                patient_name = EXCLUDED.patient_name,
                doctor_staff_id = EXCLUDED.doctor_staff_id,
                doctor_name = EXCLUDED.doctor_name,
                request_id = EXCLUDED.request_id,
                offered_options = EXCLUDED.offered_options,
                unrecognized_count = EXCLUDED.unrecognized_count,
                last_activity_at = EXCLUDED.last_activity_at
            """;

    private final JdbcTemplate jdbcTemplate;
    private final ConversationOptionsCodec codec;

    public JdbcConversationRepository(JdbcTemplate jdbcTemplate, ConversationOptionsCodec codec) {
        this.jdbcTemplate = jdbcTemplate;
        this.codec = codec;
    }

    @Override
    public Optional<Conversation> findActive(UUID clinicId, String phone) {
        return jdbcTemplate.query(FIND_ACTIVE_SQL, (row, rowNum) -> toConversation(row), clinicId, phone)
                .stream()
                .findFirst();
    }

    @Override
    public Optional<Conversation> findById(UUID conversationId) {
        return jdbcTemplate.query(FIND_BY_ID_SQL, (row, rowNum) -> toConversation(row), conversationId)
                .stream()
                .findFirst();
    }

    @Override
    public Conversation save(Conversation conversation) {
        jdbcTemplate.update(UPSERT_SQL,
                conversation.id(), conversation.clinicId(), conversation.phone(), conversation.state().name(),
                conversation.patientId(), conversation.patientName(), conversation.doctorStaffId(),
                conversation.doctorName(), conversation.requestId(), codec.encode(conversation.offeredOptions()),
                conversation.unrecognizedCount(), Timestamp.valueOf(conversation.createdAt()),
                Timestamp.valueOf(conversation.lastActivityAt()));
        return conversation;
    }

    private Conversation toConversation(ResultSet row) throws SQLException {
        return new Conversation(
                row.getObject("id", UUID.class),
                row.getObject("clinic_id", UUID.class),
                row.getString("phone"),
                ConversationState.valueOf(row.getString("state")),
                row.getObject("patient_id", UUID.class),
                row.getString("patient_name"),
                row.getObject("doctor_staff_id", UUID.class),
                row.getString("doctor_name"),
                row.getObject("request_id", UUID.class),
                codec.decode(row.getString("offered_options")),
                row.getInt("unrecognized_count"),
                row.getTimestamp("created_at").toLocalDateTime(),
                row.getTimestamp("last_activity_at").toLocalDateTime());
    }
}
