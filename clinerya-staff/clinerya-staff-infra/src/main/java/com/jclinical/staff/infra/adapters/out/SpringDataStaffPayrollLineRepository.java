package com.jclinical.staff.infra.adapters.out;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataStaffPayrollLineRepository extends JpaRepository<StaffPayrollLineEntity, UUID> {
    Optional<StaffPayrollLineEntity> findByPayrollPeriodIdAndStaffId(UUID payrollPeriodId, UUID staffId);

    List<StaffPayrollLineEntity> findByPayrollPeriodIdOrderByStaffId(UUID payrollPeriodId);
}
