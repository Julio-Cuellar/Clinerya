package com.jclinical.staff.infra.adapters.out;

import com.jclinical.staff.domain.model.StaffPayrollPeriod;
import com.jclinical.staff.domain.ports.out.StaffPayrollPeriodRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SqlStaffPayrollPeriodRepository implements StaffPayrollPeriodRepositoryPort {
    private final SpringDataStaffPayrollPeriodRepository repository;

    @Override
    public StaffPayrollPeriod save(StaffPayrollPeriod period) {
        return toDomain(repository.save(toEntity(period)));
    }

    @Override
    public Optional<StaffPayrollPeriod> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public List<StaffPayrollPeriod> findByClinicId(UUID clinicId) {
        return repository.findByClinicIdOrderByPeriodStartDesc(clinicId).stream()
                .map(this::toDomain)
                .toList();
    }

    private StaffPayrollPeriodEntity toEntity(StaffPayrollPeriod period) {
        return StaffPayrollPeriodEntity.builder()
                .id(period.getId())
                .clinicId(period.getClinicId())
                .name(period.getName())
                .periodStart(period.getPeriodStart())
                .periodEnd(period.getPeriodEnd())
                .status(period.getStatus())
                .paymentStatus(period.getPaymentStatus())
                .grossAmount(period.getGrossAmount())
                .netAmount(period.getNetAmount())
                .createdAt(period.getCreatedAt())
                .closedAt(period.getClosedAt())
                .paidAt(period.getPaidAt())
                .paymentAccountId(period.getPaymentAccountId())
                .paymentJournalEntryId(period.getPaymentJournalEntryId())
                .build();
    }

    private StaffPayrollPeriod toDomain(StaffPayrollPeriodEntity entity) {
        return StaffPayrollPeriod.builder()
                .id(entity.getId())
                .clinicId(entity.getClinicId())
                .name(entity.getName())
                .periodStart(entity.getPeriodStart())
                .periodEnd(entity.getPeriodEnd())
                .status(entity.getStatus())
                .paymentStatus(entity.getPaymentStatus())
                .grossAmount(entity.getGrossAmount())
                .netAmount(entity.getNetAmount())
                .createdAt(entity.getCreatedAt())
                .closedAt(entity.getClosedAt())
                .paidAt(entity.getPaidAt())
                .paymentAccountId(entity.getPaymentAccountId())
                .paymentJournalEntryId(entity.getPaymentJournalEntryId())
                .build();
    }
}
