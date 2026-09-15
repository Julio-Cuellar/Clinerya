package com.jclinical.staff.domain.ports.out;

import com.jclinical.staff.domain.model.StaffCompensation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StaffCompensationRepositoryPort {

    StaffCompensation save(StaffCompensation compensation);

    Optional<StaffCompensation> findByStaffId(UUID staffId);

    List<StaffCompensation> findByClinicId(UUID clinicId);
}
