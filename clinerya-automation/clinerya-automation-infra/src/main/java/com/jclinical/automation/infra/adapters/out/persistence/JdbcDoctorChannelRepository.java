package com.jclinical.automation.infra.adapters.out.persistence;

import com.jclinical.automation.domain.model.DoctorChannel;
import com.jclinical.automation.domain.ports.out.DoctorChannelRepositoryPort;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public class JdbcDoctorChannelRepository implements DoctorChannelRepositoryPort {

    private static final String COLUMNS = "clinic_id, staff_id, phone, active, consent_at, updated_by, updated_at";

    private static final String FIND_SQL =
            "SELECT " + COLUMNS + " FROM automation.doctor_channels WHERE clinic_id = ? AND staff_id = ?";

    private static final String FIND_ACTIVE_BY_PHONE_SQL =
            "SELECT " + COLUMNS + " FROM automation.doctor_channels WHERE clinic_id = ? AND phone = ? AND active LIMIT 1";

    private static final String UPSERT_SQL = """
            INSERT INTO automation.doctor_channels (clinic_id, staff_id, phone, active, consent_at, updated_by, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (clinic_id, staff_id) DO UPDATE
               SET phone = EXCLUDED.phone, active = EXCLUDED.active, consent_at = EXCLUDED.consent_at,
                   updated_by = EXCLUDED.updated_by, updated_at = EXCLUDED.updated_at
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcDoctorChannelRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<DoctorChannel> find(UUID clinicId, UUID staffId) {
        return jdbcTemplate.query(FIND_SQL, (row, rowNum) -> toChannel(row), clinicId, staffId).stream().findFirst();
    }

    @Override
    public Optional<DoctorChannel> findActiveByPhone(UUID clinicId, String phone) {
        return jdbcTemplate.query(FIND_ACTIVE_BY_PHONE_SQL, (row, rowNum) -> toChannel(row), clinicId, phone)
                .stream()
                .findFirst();
    }

    @Override
    public DoctorChannel save(DoctorChannel channel) {
        jdbcTemplate.update(UPSERT_SQL, channel.clinicId(), channel.staffId(), channel.phone(), channel.active(),
                timestamp(channel.consentAt()), channel.updatedBy(), Timestamp.valueOf(channel.updatedAt()));
        return channel;
    }

    private static DoctorChannel toChannel(ResultSet row) throws SQLException {
        Timestamp consentAt = row.getTimestamp("consent_at");
        return new DoctorChannel(
                row.getObject("clinic_id", UUID.class),
                row.getObject("staff_id", UUID.class),
                row.getString("phone"),
                row.getBoolean("active"),
                consentAt == null ? null : consentAt.toLocalDateTime(),
                row.getObject("updated_by", UUID.class),
                row.getTimestamp("updated_at").toLocalDateTime());
    }

    private static Timestamp timestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }
}
