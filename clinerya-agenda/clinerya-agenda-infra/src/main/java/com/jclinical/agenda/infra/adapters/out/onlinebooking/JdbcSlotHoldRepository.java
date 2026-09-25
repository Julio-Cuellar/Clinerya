package com.jclinical.agenda.infra.adapters.out.onlinebooking;

import com.jclinical.agenda.domain.model.SlotHold;
import com.jclinical.agenda.domain.ports.out.SlotHoldRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class JdbcSlotHoldRepository implements SlotHoldRepositoryPort {

    private static final String COLUMNS =
            "id, clinic_id, doctor_staff_id, start_at, end_at, expires_at, reference, status, created_at";

    private static final String UPSERT_SQL = """
            INSERT INTO agenda.slot_holds (%s)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET status = EXCLUDED.status, expires_at = EXCLUDED.expires_at
            """.formatted(COLUMNS);

    private static final String FIND_ACTIVE_SQL = """
            SELECT %s FROM agenda.slot_holds
             WHERE doctor_staff_id = ? AND clinic_id = ? AND status = 'ACTIVE' AND expires_at > ?
               AND start_at < ? AND end_at > ?
            """.formatted(COLUMNS);

    private final JdbcTemplate jdbcTemplate;

    @Override
    public SlotHold save(SlotHold hold) {
        jdbcTemplate.update(UPSERT_SQL, hold.id(), hold.clinicId(), hold.doctorStaffId(),
                Timestamp.valueOf(hold.start()), Timestamp.valueOf(hold.end()), Timestamp.valueOf(hold.expiresAt()),
                hold.reference(), hold.status().name(), Timestamp.valueOf(hold.createdAt()));
        return hold;
    }

    @Override
    public Optional<SlotHold> findByIdAndClinicId(UUID holdId, UUID clinicId) {
        return jdbcTemplate.query("SELECT " + COLUMNS + " FROM agenda.slot_holds WHERE id = ? AND clinic_id = ?",
                (row, rowNum) -> toHold(row), holdId, clinicId).stream().findFirst();
    }

    @Override
    public List<SlotHold> findActiveByDoctorAndRange(UUID doctorStaffId, UUID clinicId, LocalDateTime from,
                                                     LocalDateTime to, LocalDateTime now) {
        return jdbcTemplate.query(FIND_ACTIVE_SQL, (row, rowNum) -> toHold(row), doctorStaffId, clinicId,
                Timestamp.valueOf(now), Timestamp.valueOf(to), Timestamp.valueOf(from));
    }

    /** Bloqueo transaccional por medico: se libera solo al terminar la transaccion. */
    @Override
    public void lockDoctorSchedule(UUID clinicId, UUID doctorStaffId) {
        jdbcTemplate.query("SELECT pg_advisory_xact_lock(hashtextextended(?, 0))",
                resultSet -> null, "agenda-doctor:" + clinicId + ":" + doctorStaffId);
    }

    private static SlotHold toHold(ResultSet row) throws SQLException {
        return new SlotHold(
                row.getObject("id", UUID.class),
                row.getObject("clinic_id", UUID.class),
                row.getObject("doctor_staff_id", UUID.class),
                row.getTimestamp("start_at").toLocalDateTime(),
                row.getTimestamp("end_at").toLocalDateTime(),
                row.getTimestamp("expires_at").toLocalDateTime(),
                row.getObject("reference", UUID.class),
                SlotHold.Status.valueOf(row.getString("status")),
                row.getTimestamp("created_at").toLocalDateTime());
    }
}
