package com.jclinical.staff.domain.ports.out;

import com.jclinical.staff.domain.model.StaffActivityLog;
import com.jclinical.staff.domain.model.StaffActivityType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface StaffActivityRepositoryPort {
    StaffActivityLog save(StaffActivityLog activity);

    List<StaffActivityLog> findByClinicIdAndOccurredAtBetween(UUID clinicId, LocalDateTime from, LocalDateTime to);

    List<StaffActivityLog> findByClinicIdAndStaffIdAndOccurredAtBetween(UUID clinicId, UUID staffId, LocalDateTime from, LocalDateTime to);

    List<StaffActivityLog> findByClinicIdAndTypeAndOccurredAtBetween(UUID clinicId, StaffActivityType type, LocalDateTime from, LocalDateTime to);
}
