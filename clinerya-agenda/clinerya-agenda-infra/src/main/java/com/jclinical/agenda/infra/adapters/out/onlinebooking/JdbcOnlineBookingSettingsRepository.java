package com.jclinical.agenda.infra.adapters.out.onlinebooking;

import com.jclinical.agenda.domain.ports.out.OnlineBookingSettingsRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class JdbcOnlineBookingSettingsRepository implements OnlineBookingSettingsRepositoryPort {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Optional<Integer> findSlotMinutes(UUID clinicId) {
        return jdbcTemplate.queryForList("SELECT slot_minutes FROM agenda.online_booking_settings WHERE clinic_id = ?",
                Integer.class, clinicId).stream().findFirst();
    }

    @Override
    public void saveSlotMinutes(UUID clinicId, int minutes) {
        jdbcTemplate.update("""
                INSERT INTO agenda.online_booking_settings (clinic_id, slot_minutes, updated_at)
                VALUES (?, ?, CURRENT_TIMESTAMP)
                ON CONFLICT (clinic_id) DO UPDATE SET slot_minutes = EXCLUDED.slot_minutes, updated_at = CURRENT_TIMESTAMP
                """, clinicId, minutes);
    }

    @Override
    public Optional<Integer> findLeadMinutes(UUID clinicId, UUID doctorStaffId) {
        return jdbcTemplate.queryForList("""
                SELECT min_lead_minutes FROM agenda.doctor_booking_preferences
                 WHERE clinic_id = ? AND doctor_staff_id = ?
                """, Integer.class, clinicId, doctorStaffId).stream().findFirst();
    }

    @Override
    public void saveLeadMinutes(UUID clinicId, UUID doctorStaffId, int minutes) {
        jdbcTemplate.update("""
                INSERT INTO agenda.doctor_booking_preferences (clinic_id, doctor_staff_id, min_lead_minutes, updated_at)
                VALUES (?, ?, ?, CURRENT_TIMESTAMP)
                ON CONFLICT (clinic_id, doctor_staff_id)
                DO UPDATE SET min_lead_minutes = EXCLUDED.min_lead_minutes, updated_at = CURRENT_TIMESTAMP
                """, clinicId, doctorStaffId, minutes);
    }
}
