package com.jclinical.staff.infra.adapters.out;

import com.jclinical.staff.domain.model.ClinicStaffInvitation;
import com.jclinical.staff.domain.model.StaffRole;
import com.jclinical.staff.domain.ports.out.ClinicStaffInvitationRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class SqlClinicStaffInvitationRepository implements ClinicStaffInvitationRepositoryPort {

    private final SpringDataClinicStaffInvitationRepository springDataRepository;

    @Override
    public ClinicStaffInvitation save(ClinicStaffInvitation invitation) {
        ClinicStaffInvitationEntity entity = toEntity(invitation);
        ClinicStaffInvitationEntity saved = springDataRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<ClinicStaffInvitation> findByTokenAndUsedFalse(String token) {
        return springDataRepository.findByTokenAndUsedFalse(token)
                .map(this::toDomain);
    }

    @Override
    public List<ClinicStaffInvitation> findByClinicIdAndUsedFalse(UUID clinicId) {
        return springDataRepository.findByClinicIdAndUsedFalse(clinicId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<ClinicStaffInvitation> findByClinicIdAndEmailAndUsedFalse(UUID clinicId, String email) {
        return springDataRepository.findByClinicIdAndEmailAndUsedFalse(clinicId, email)
                .map(this::toDomain);
    }

    private ClinicStaffInvitationEntity toEntity(ClinicStaffInvitation domain) {
        if (domain == null) return null;
        return ClinicStaffInvitationEntity.builder()
                .id(domain.getId())
                .clinicId(domain.getClinicId())
                .email(domain.getEmail())
                .role(domain.getRole().name())
                .token(domain.getToken())
                .used(domain.isUsed())
                .createdAt(domain.getCreatedAt())
                .expiresAt(domain.getExpiresAt())
                .build();
    }

    private ClinicStaffInvitation toDomain(ClinicStaffInvitationEntity entity) {
        if (entity == null) return null;
        return ClinicStaffInvitation.builder()
                .id(entity.getId())
                .clinicId(entity.getClinicId())
                .email(entity.getEmail())
                .role(StaffRole.valueOf(entity.getRole()))
                .token(entity.getToken())
                .used(entity.isUsed())
                .createdAt(entity.getCreatedAt())
                .expiresAt(entity.getExpiresAt())
                .build();
    }
}
