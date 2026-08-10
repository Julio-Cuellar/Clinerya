package com.jclinical.staff.infra.adapters.out;

import com.jclinical.staff.domain.model.StaffPayrollLine;
import com.jclinical.staff.domain.ports.out.StaffPayrollLineRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SqlStaffPayrollLineRepository implements StaffPayrollLineRepositoryPort {
    private final SpringDataStaffPayrollLineRepository repository;

    @Override
    public StaffPayrollLine save(StaffPayrollLine line) {
        return toDomain(repository.save(toEntity(line)));
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }

    @Override
    public Optional<StaffPayrollLine> findByPeriodIdAndStaffId(UUID payrollPeriodId, UUID staffId) {
        return repository.findByPayrollPeriodIdAndStaffId(payrollPeriodId, staffId).map(this::toDomain);
    }

    @Override
    public List<StaffPayrollLine> findByPeriodId(UUID payrollPeriodId) {
        return repository.findByPayrollPeriodIdOrderByStaffId(payrollPeriodId).stream()
                .map(this::toDomain)
                .toList();
    }

    private StaffPayrollLineEntity toEntity(StaffPayrollLine line) {
        return StaffPayrollLineEntity.builder()
                .id(line.getId())
                .clinicId(line.getClinicId())
                .payrollPeriodId(line.getPayrollPeriodId())
                .staffId(line.getStaffId())
                .baseSalary(line.getBaseSalary())
                .commissionAmount(line.getCommissionAmount())
                .bonusAmount(line.getBonusAmount())
                .deductionAmount(line.getDeductionAmount())
                .grossAmount(line.getGrossAmount())
                .netAmount(line.getNetAmount())
                .notes(line.getNotes())
                .createdAt(line.getCreatedAt())
                .updatedAt(line.getUpdatedAt())
                .build();
    }

    private StaffPayrollLine toDomain(StaffPayrollLineEntity entity) {
        return StaffPayrollLine.builder()
                .id(entity.getId())
                .clinicId(entity.getClinicId())
                .payrollPeriodId(entity.getPayrollPeriodId())
                .staffId(entity.getStaffId())
                .baseSalary(entity.getBaseSalary())
                .commissionAmount(entity.getCommissionAmount())
                .bonusAmount(entity.getBonusAmount())
                .deductionAmount(entity.getDeductionAmount())
                .grossAmount(entity.getGrossAmount())
                .netAmount(entity.getNetAmount())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
