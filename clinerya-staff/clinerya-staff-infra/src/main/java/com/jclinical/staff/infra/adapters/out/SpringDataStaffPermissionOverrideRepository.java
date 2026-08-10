package com.jclinical.staff.infra.adapters.out;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataStaffPermissionOverrideRepository extends JpaRepository<StaffPermissionOverrideEntity, UUID> {
    List<StaffPermissionOverrideEntity> findByClinicIdAndStaffId(UUID clinicId, UUID staffId);
    Optional<StaffPermissionOverrideEntity> findByClinicIdAndStaffIdAndPermissionCode(UUID clinicId, UUID staffId, String permissionCode);
}
