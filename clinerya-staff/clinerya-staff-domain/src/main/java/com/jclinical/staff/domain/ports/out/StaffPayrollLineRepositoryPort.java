package com.jclinical.staff.domain.ports.out;

import com.jclinical.staff.domain.model.StaffPayrollLine;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StaffPayrollLineRepositoryPort {
    StaffPayrollLine save(StaffPayrollLine line);

    void deleteById(UUID id);

    Optional<StaffPayrollLine> findByPeriodIdAndStaffId(UUID payrollPeriodId, UUID staffId);

    List<StaffPayrollLine> findByPeriodId(UUID payrollPeriodId);
}
