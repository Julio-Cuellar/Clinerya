package com.jclinical.automation.infra.adapters.out.persistence;

import com.jclinical.automation.domain.model.PendingAction;
import com.jclinical.automation.domain.ports.out.PendingActionPort;
import com.jclinical.core.security.crypto.FieldCipher;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * La accion que el agente propuso y espera confirmacion (una por conversacion; proponer otra la
 * reemplaza). El nombre del paciente y el motivo que dio se guardan cifrados.
 */
public class JdbcPendingActionRepository implements PendingActionPort {

    private static final String COLUMNS = "conversation_id, kind, patient_id, patient_name, doctor_staff_id, doctor_name, "
            + "start_at, end_at, appointment_id, note, proposed_at, service_id";

    private static final String FIND_SQL =
            "SELECT " + COLUMNS + " FROM automation.agent_pending_actions WHERE conversation_id = ?";

    private static final String UPSERT_SQL = """
            INSERT INTO automation.agent_pending_actions (%s)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (conversation_id) DO UPDATE
               SET kind = EXCLUDED.kind, patient_id = EXCLUDED.patient_id, patient_name = EXCLUDED.patient_name,
                   doctor_staff_id = EXCLUDED.doctor_staff_id, doctor_name = EXCLUDED.doctor_name,
                   start_at = EXCLUDED.start_at, end_at = EXCLUDED.end_at, appointment_id = EXCLUDED.appointment_id,
                   note = EXCLUDED.note, proposed_at = EXCLUDED.proposed_at, service_id = EXCLUDED.service_id
            """.formatted(COLUMNS);

    private static final String DELETE_SQL = "DELETE FROM automation.agent_pending_actions WHERE conversation_id = ?";
    private static final String DELETE_STALE_SQL = "DELETE FROM automation.agent_pending_actions WHERE proposed_at < ?";

    private final JdbcTemplate jdbcTemplate;
    private final FieldCipher cipher;

    public JdbcPendingActionRepository(JdbcTemplate jdbcTemplate, FieldCipher cipher) {
        this.jdbcTemplate = jdbcTemplate;
        this.cipher = cipher;
    }

    @Override
    public void save(PendingAction action) {
        jdbcTemplate.update(UPSERT_SQL, action.conversationId(), action.kind().name(), action.patientId(),
                encrypt(action.patientName()), action.doctorStaffId(), action.doctorName(), timestamp(action.start()),
                timestamp(action.end()), action.appointmentId(), encrypt(action.note()), timestamp(action.proposedAt()),
                action.serviceId());
    }

    @Override
    public Optional<PendingAction> find(UUID conversationId) {
        return jdbcTemplate.query(FIND_SQL, (row, rowNum) -> toAction(row), conversationId).stream().findFirst();
    }

    @Override
    public void clear(UUID conversationId) {
        jdbcTemplate.update(DELETE_SQL, conversationId);
    }

    /** Propuestas que ya vencieron y nadie confirmo: no se guardan datos personales de mas. */
    public int deleteStale(LocalDateTime before) {
        return jdbcTemplate.update(DELETE_STALE_SQL, Timestamp.valueOf(before));
    }

    private PendingAction toAction(ResultSet row) throws SQLException {
        return new PendingAction(
                row.getObject("conversation_id", UUID.class),
                PendingAction.Kind.valueOf(row.getString("kind")),
                row.getObject("patient_id", UUID.class),
                decrypt(row.getString("patient_name")),
                row.getObject("doctor_staff_id", UUID.class),
                row.getString("doctor_name"),
                localDateTime(row.getTimestamp("start_at")),
                localDateTime(row.getTimestamp("end_at")),
                row.getObject("appointment_id", UUID.class),
                row.getTimestamp("proposed_at").toLocalDateTime(),
                decrypt(row.getString("note")),
                row.getObject("service_id", UUID.class));
    }

    private String encrypt(String value) {
        return value == null ? null : cipher.encrypt(value);
    }

    private String decrypt(String value) {
        return value == null ? null : cipher.decrypt(value);
    }

    private static Timestamp timestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    private static LocalDateTime localDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }
}
