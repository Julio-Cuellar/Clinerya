package com.jclinical.staff.infra.adapters.out;

import com.jclinical.staff.domain.model.StaffInvitationCompensation;
import com.jclinical.staff.domain.model.StaffPayFrequency;
import com.jclinical.staff.domain.model.StaffPaymentMethod;
import com.jclinical.staff.domain.ports.out.StaffInvitationCompensationRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SqlStaffInvitationCompensationRepository implements StaffInvitationCompensationRepositoryPort {

    private final SpringDataStaffInvitationCompensationRepository repository;

    @Override
    public StaffInvitationCompensation save(StaffInvitationCompensation compensation) {
        return toDomain(repository.save(toEntity(compensation)));
    }

    @Override
    public Optional<StaffInvitationCompensation> findByInvitationId(UUID invitationId) {
        return repository.findById(invitationId).map(this::toDomain);
    }

    @Override
    public void deleteByInvitationId(UUID invitationId) {
        repository.deleteById(invitationId);
    }

    private StaffInvitationCompensationEntity toEntity(StaffInvitationCompensation compensation) {
        return StaffInvitationCompensationEntity.builder()
                .invitationId(compensation.getInvitationId())
                .clinicId(compensation.getClinicId())
                .baseSalary(compensation.getBaseSalary())
                .payFrequency(compensation.getPayFrequency() != null ? compensation.getPayFrequency().name() : StaffPayFrequency.BIWEEKLY.name())
                .paymentMethod(compensation.getPaymentMethod() != null ? compensation.getPaymentMethod().name() : StaffPaymentMethod.BANK_TRANSFER.name())
                .paymentAccountClabe(compensation.getPaymentAccountClabe())
                .rfc(compensation.getRfc())
                .curp(compensation.getCurp())
                .nss(compensation.getNss())
                .createdAt(compensation.getCreatedAt())
                .build();
    }

    private StaffInvitationCompensation toDomain(StaffInvitationCompensationEntity entity) {
        return StaffInvitationCompensation.builder()
                .invitationId(entity.getInvitationId())
                .clinicId(entity.getClinicId())
                .baseSalary(entity.getBaseSalary())
                .payFrequency(entity.getPayFrequency() != null ? StaffPayFrequency.valueOf(entity.getPayFrequency()) : null)
                .paymentMethod(entity.getPaymentMethod() != null ? StaffPaymentMethod.valueOf(entity.getPaymentMethod()) : null)
                .paymentAccountClabe(entity.getPaymentAccountClabe())
                .rfc(entity.getRfc())
                .curp(entity.getCurp())
                .nss(entity.getNss())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
