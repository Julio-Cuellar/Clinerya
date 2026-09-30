package com.jclinical.automation.infra.adapters.out.persistence;

import com.jclinical.automation.domain.model.AppointmentReminder;
import com.jclinical.automation.domain.ports.out.AppointmentReminderRepositoryPort;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Recordatorios de cita, uno por cita; reprogramar reemplaza el anterior y lo deja por enviar otra vez. */
public class JdbcAppointmentReminderRepository implements AppointmentReminderRepositoryPort {

    private static final String COLUMNS =
            "appointment_id, clinic_id, patient_id, doctor_staff_id, starts_at, service_name, send_at, sent_at";

    private static final String FIND_SQL = "SELECT " + COLUMNS
            + " FROM automation.appointment_reminders WHERE appointment_id = ?";

    private static final String UPSERT_SQL = """
            INSERT INTO automation.appointment_reminders
                (appointment_id, clinic_id, patient_id, doctor_staff_id, starts_at, service_name, send_at, sent_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, now())
            ON CONFLICT (appointment_id) DO UPDATE
               SET clinic_id = EXCLUDED.clinic_id, patient_id = EXCLUDED.patient_id,
                   doctor_staff_id = EXCLUDED.doctor_staff_id, starts_at = EXCLUDED.starts_at,
                   service_name = EXCLUDED.service_name, send_at = EXCLUDED.send_at, sent_at = EXCLUDED.sent_at,
                   updated_at = now()
            """;

    private static final String DELETE_SQL = "DELETE FROM automation.appointment_reminders WHERE appointment_id = ?";

    private static final String DUE_SQL = "SELECT " + COLUMNS + """
             FROM automation.appointment_reminders
            WHERE sent_at IS NULL AND send_at <= ?
            ORDER BY send_at
            LIMIT ?
            """;

    private static final String MARK_SENT_SQL = """
            UPDATE automation.appointment_reminders SET sent_at = ?, updated_at = now()
             WHERE appointment_id = ? AND sent_at IS NULL
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcAppointmentReminderRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<AppointmentReminder> find(UUID appointmentId) {
        return jdbcTemplate.query(FIND_SQL, (row, rowNum) -> toReminder(row), appointmentId).stream().findFirst();
    }

    @Override
    public void save(AppointmentReminder reminder) {
        jdbcTemplate.update(UPSERT_SQL, reminder.appointmentId(), reminder.clinicId(), reminder.patientId(),
                reminder.doctorStaffId(), Timestamp.valueOf(reminder.startsAt()), reminder.serviceName(),
                Timestamp.valueOf(reminder.sendAt()), timestamp(reminder.sentAt()));
    }

    @Override
    public void delete(UUID appointmentId) {
        jdbcTemplate.update(DELETE_SQL, appointmentId);
    }

    @Override
    public List<AppointmentReminder> findDue(LocalDateTime now, int limit) {
        return jdbcTemplate.query(DUE_SQL, (row, rowNum) -> toReminder(row), Timestamp.valueOf(now), limit);
    }

    @Override
    public boolean markSent(UUID appointmentId, LocalDateTime at) {
        return jdbcTemplate.update(MARK_SENT_SQL, Timestamp.valueOf(at), appointmentId) == 1;
    }

    private static AppointmentReminder toReminder(ResultSet row) throws SQLException {
        Timestamp sentAt = row.getTimestamp("sent_at");
        return new AppointmentReminder(
                row.getObject("appointment_id", UUID.class),
                row.getObject("clinic_id", UUID.class),
                row.getObject("patient_id", UUID.class),
                row.getObject("doctor_staff_id", UUID.class),
                row.getTimestamp("starts_at").toLocalDateTime(),
                row.getString("service_name"),
                row.getTimestamp("send_at").toLocalDateTime(),
                sentAt == null ? null : sentAt.toLocalDateTime());
    }

    private static Timestamp timestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }
}
