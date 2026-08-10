package com.jclinical.staff.domain.ports.out;

import com.jclinical.staff.domain.model.StaffPermission;
import com.jclinical.staff.domain.model.StaffPermissionOverride;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StaffPermissionOverrideRepositoryPort {
    StaffPermissionOverride save(StaffPermissionOverride override);
    List<StaffPermissionOverride> findByClinicIdAndStaffId(UUID clinicId, UUID staffId);
    Optional<StaffPermissionOverride> findByClinicIdAndStaffIdAndPermission(UUID clinicId, UUID staffId, StaffPermission permission);
    void delete(StaffPermissionOverride override);
}
