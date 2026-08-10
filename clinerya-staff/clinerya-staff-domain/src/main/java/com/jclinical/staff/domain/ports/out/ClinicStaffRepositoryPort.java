package com.jclinical.staff.domain.ports.out;

import com.jclinical.staff.domain.model.ClinicStaff;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClinicStaffRepositoryPort {
    ClinicStaff save(ClinicStaff staff);
    Optional<ClinicStaff> findById(UUID id);
    Optional<ClinicStaff> findByClinicIdAndUserId(UUID clinicId, UUID userId);
    List<ClinicStaff> findByClinicId(UUID clinicId);
    List<ClinicStaff> findByUserId(UUID userId);
}
