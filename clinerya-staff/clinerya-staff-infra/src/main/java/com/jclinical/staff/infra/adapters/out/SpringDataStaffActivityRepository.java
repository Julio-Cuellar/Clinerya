package com.jclinical.staff.infra.adapters.out;

import com.jclinical.staff.domain.model.StaffActivityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataStaffActivityRepository extends JpaRepository<StaffActivityLogEntity, UUID> {
    List<StaffActivityLogEntity> findByClinicIdAndOccurredAtBetweenOrderByOccurredAtDesc(
            UUID clinicId, LocalDateTime from, LocalDateTime to);

    List<StaffActivityLogEntity> findByClinicIdAndStaffIdAndOccurredAtBetweenOrderByOccurredAtDesc(
            UUID clinicId, UUID staffId, LocalDateTime from, LocalDateTime to);

    List<StaffActivityLogEntity> findByClinicIdAndTypeAndOccurredAtBetweenOrderByOccurredAtDesc(
            UUID clinicId, StaffActivityType type, LocalDateTime from, LocalDateTime to);
}
