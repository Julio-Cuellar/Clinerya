package com.jclinical.staff.domain.ports.out;

import com.jclinical.staff.domain.model.StaffAttendanceEntry;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StaffAttendanceRepositoryPort {
    StaffAttendanceEntry save(StaffAttendanceEntry entry);

    Optional<StaffAttendanceEntry> findById(UUID id);

    Optional<StaffAttendanceEntry> findOpenByStaffId(UUID clinicId, UUID staffId);

    List<StaffAttendanceEntry> findByClinicIdAndWorkDateBetween(UUID clinicId, LocalDate from, LocalDate to);
}
