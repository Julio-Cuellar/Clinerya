package com.jclinical.staff.domain.ports.out;

import com.jclinical.staff.domain.model.StaffPayrollPeriod;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StaffPayrollPeriodRepositoryPort {
    StaffPayrollPeriod save(StaffPayrollPeriod period);

    Optional<StaffPayrollPeriod> findById(UUID id);

    List<StaffPayrollPeriod> findByClinicId(UUID clinicId);
}
