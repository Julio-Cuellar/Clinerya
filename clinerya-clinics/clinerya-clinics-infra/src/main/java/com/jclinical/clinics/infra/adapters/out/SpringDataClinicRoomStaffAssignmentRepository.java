package com.jclinical.clinics.infra.adapters.out;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataClinicRoomStaffAssignmentRepository extends JpaRepository<ClinicRoomStaffAssignmentEntity, UUID> {
    Optional<ClinicRoomStaffAssignmentEntity> findByClinicIdAndRoomIdAndStaffId(UUID clinicId, UUID roomId, UUID staffId);
    List<ClinicRoomStaffAssignmentEntity> findByClinicIdAndRoomIdAndActiveTrueOrderByAssignedAtAsc(UUID clinicId, UUID roomId);
}
