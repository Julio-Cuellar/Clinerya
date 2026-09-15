package com.jclinical.staff.infra.adapters.out;

import com.jclinical.staff.domain.model.StaffCompensation;
import com.jclinical.staff.domain.model.StaffPayFrequency;
import com.jclinical.staff.domain.model.StaffPaymentMethod;
import com.jclinical.staff.domain.ports.out.StaffCompensationRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SqlStaffCompensationRepository implements StaffCompensationRepositoryPort {

    private final SpringDataStaffCompensationRepository repository;

    @Override
    public StaffCompensation save(StaffCompensation compensation) {
        return toDomain(repository.save(toEntity(compensation)));
    }

    @Override
    public Optional<StaffCompensation> findByStaffId(UUID staffId) {
        return repository.findById(staffId).map(this::toDomain);
    }

    @Override
    public List<StaffCompensation> findByClinicId(UUID clinicId) {
        return repository.findByClinicId(clinicId).stream().map(this::toDomain).toList();
    }

    private StaffCompensationEntity toEntity(StaffCompensation compensation) {
        return StaffCompensationEntity.builder()
                .staffId(compensation.getStaffId())
                .clinicId(compensation.getClinicId())
                .baseSalary(compensation.getBaseSalary())
                .payFrequency(compensation.getPayFrequency() != null ? compensation.getPayFrequency().name() : null)
                .paymentMethod(compensation.getPaymentMethod() != null ? compensation.getPaymentMethod().name() : null)
                .paymentAccountClabe(compensation.getPaymentAccountClabe())
                .rfc(compensation.getRfc())
                .curp(compensation.getCurp())
                .nss(compensation.getNss())
                .createdAt(compensation.getCreatedAt())
                .updatedAt(compensation.getUpdatedAt())
                .build();
    }

    private StaffCompensation toDomain(StaffCompensationEntity entity) {
        return StaffCompensation.builder()
                .staffId(entity.getStaffId())
                .clinicId(entity.getClinicId())
                .baseSalary(entity.getBaseSalary())
                .payFrequency(entity.getPayFrequency() != null ? StaffPayFrequency.valueOf(entity.getPayFrequency()) : null)
                .paymentMethod(entity.getPaymentMethod() != null ? StaffPaymentMethod.valueOf(entity.getPaymentMethod()) : null)
                .paymentAccountClabe(entity.getPaymentAccountClabe())
                .rfc(entity.getRfc())
                .curp(entity.getCurp())
                .nss(entity.getNss())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
