package com.jclinical.automation.infra.adapters.out.persistence;

import com.jclinical.automation.domain.model.AppointmentRequest;
import com.jclinical.automation.domain.model.AppointmentRequest.Status;
import com.jclinical.automation.domain.ports.out.AppointmentRequestRepositoryPort;
import com.jclinical.automation.domain.ports.out.PendingRequestReminderPort;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class JdbcAppointmentRequestRepository implements AppointmentRequestRepositoryPort, PendingRequestReminderPort {

    /** Tope de filas por consulta: la bandeja y el barrido de vencimientos trabajan por lotes. */
    static final int MAX_ROWS = 200;

    private static final String COLUMNS = """
            id, clinic_id, conversation_id, patient_id, patient_name, patient_phone, doctor_staff_id,
            doctor_name, start_at, end_at, hold_id, status, proposed_options, appointment_id,
            rejection_reason, created_at, responded_at
            """;

    private static final String UPSERT_SQL = """
            INSERT INTO automation.appointment_requests (%s)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET
                status = EXCLUDED.status,
                proposed_options = EXCLUDED.proposed_options,
                appointment_id = EXCLUDED.appointment_id,
                rejection_reason = EXCLUDED.rejection_reason,
                responded_at = EXCLUDED.responded_at
            """.formatted(COLUMNS);

    private static final String FIND_BY_ID_SQL =
            "SELECT " + COLUMNS + " FROM automation.appointment_requests WHERE id = ? AND clinic_id = ?";

    private static final String FIND_PENDING_BY_DOCTOR_SQL = "SELECT " + COLUMNS + """
             FROM automation.appointment_requests
            WHERE clinic_id = ? AND doctor_staff_id = ? AND status = 'PENDING'
            ORDER BY created_at
            LIMIT %d
            """.formatted(MAX_ROWS);

    private static final String FIND_OVERDUE_SQL = "SELECT " + COLUMNS + """
             FROM automation.appointment_requests
            WHERE (status = 'PENDING' AND created_at <= ?)
               OR (status = 'OPTIONS_PROPOSED' AND responded_at <= ?)
            ORDER BY created_at
            LIMIT %d
            """.formatted(MAX_ROWS);

    private static final String FIND_TO_REMIND_SQL = "SELECT " + COLUMNS + """
             FROM automation.appointment_requests
            WHERE status = 'PENDING' AND reminded_at IS NULL AND created_at <= ?
            ORDER BY created_at
            LIMIT %d
            """.formatted(MAX_ROWS);

    private static final String MARK_REMINDED_SQL =
            "UPDATE automation.appointment_requests SET reminded_at = ? WHERE id = ? AND reminded_at IS NULL";

    private final JdbcTemplate jdbcTemplate;
    private final ProposedOptionsCodec codec;

    public JdbcAppointmentRequestRepository(JdbcTemplate jdbcTemplate, ProposedOptionsCodec codec) {
        this.jdbcTemplate = jdbcTemplate;
        this.codec = codec;
    }

    @Override
    public AppointmentRequest save(AppointmentRequest request) {
        jdbcTemplate.update(UPSERT_SQL,
                request.id(), request.clinicId(), request.conversationId(), request.patientId(),
                request.patientName(), request.patientPhone(), request.doctorStaffId(), request.doctorName(),
                Timestamp.valueOf(request.start()), Timestamp.valueOf(request.end()), request.holdId(),
                request.status().name(), codec.encode(request.proposedOptions()), request.appointmentId(),
                request.rejectionReason(), Timestamp.valueOf(request.createdAt()), timestamp(request.respondedAt()));
        return request;
    }

    @Override
    public Optional<AppointmentRequest> findByIdAndClinicId(UUID requestId, UUID clinicId) {
        return jdbcTemplate.query(FIND_BY_ID_SQL, (row, rowNum) -> toRequest(row), requestId, clinicId)
                .stream()
                .findFirst();
    }

    @Override
    public List<AppointmentRequest> findPendingByDoctor(UUID clinicId, UUID doctorStaffId) {
        return jdbcTemplate.query(FIND_PENDING_BY_DOCTOR_SQL, (row, rowNum) -> toRequest(row), clinicId, doctorStaffId);
    }

    @Override
    public List<AppointmentRequest> findOverdue(LocalDateTime cutoff) {
        Timestamp limit = Timestamp.valueOf(cutoff);
        return jdbcTemplate.query(FIND_OVERDUE_SQL, (row, rowNum) -> toRequest(row), limit, limit);
    }

    @Override
    public List<AppointmentRequest> findPendingNotRemindedBefore(LocalDateTime createdBefore) {
        return jdbcTemplate.query(FIND_TO_REMIND_SQL, (row, rowNum) -> toRequest(row), Timestamp.valueOf(createdBefore));
    }

    @Override
    public void markReminded(UUID requestId, LocalDateTime at) {
        jdbcTemplate.update(MARK_REMINDED_SQL, Timestamp.valueOf(at), requestId);
    }

    private AppointmentRequest toRequest(ResultSet row) throws SQLException {
        return new AppointmentRequest(
                row.getObject("id", UUID.class),
                row.getObject("clinic_id", UUID.class),
                row.getObject("conversation_id", UUID.class),
                row.getObject("patient_id", UUID.class),
                row.getString("patient_name"),
                row.getString("patient_phone"),
                row.getObject("doctor_staff_id", UUID.class),
                row.getString("doctor_name"),
                row.getTimestamp("start_at").toLocalDateTime(),
                row.getTimestamp("end_at").toLocalDateTime(),
                row.getObject("hold_id", UUID.class),
                Status.valueOf(row.getString("status")),
                codec.decode(row.getString("proposed_options")),
                row.getObject("appointment_id", UUID.class),
                row.getString("rejection_reason"),
                row.getTimestamp("created_at").toLocalDateTime(),
                localDateTime(row.getTimestamp("responded_at")));
    }

    private static Timestamp timestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    private static LocalDateTime localDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }
}
