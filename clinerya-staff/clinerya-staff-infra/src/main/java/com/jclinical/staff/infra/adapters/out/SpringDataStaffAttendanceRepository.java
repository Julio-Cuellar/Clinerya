package com.jclinical.staff.infra.adapters.out;

import com.jclinical.staff.domain.model.StaffAttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataStaffAttendanceRepository extends JpaRepository<StaffAttendanceEntryEntity, UUID> {
    Optional<StaffAttendanceEntryEntity> findByClinicIdAndStaffIdAndStatus(UUID clinicId, UUID staffId, StaffAttendanceStatus status);

    List<StaffAttendanceEntryEntity> findByClinicIdAndWorkDateBetweenOrderByWorkDateDescClockInAtDesc(
            UUID clinicId, LocalDate from, LocalDate to);
}
