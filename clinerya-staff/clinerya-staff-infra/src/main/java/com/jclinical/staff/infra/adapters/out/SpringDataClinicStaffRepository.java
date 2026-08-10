package com.jclinical.staff.infra.adapters.out;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataClinicStaffRepository extends JpaRepository<ClinicStaffEntity, UUID> {
    Optional<ClinicStaffEntity> findByClinicIdAndUserId(UUID clinicId, UUID userId);
    List<ClinicStaffEntity> findByClinicId(UUID clinicId);
    List<ClinicStaffEntity> findByUserId(UUID userId);
}
