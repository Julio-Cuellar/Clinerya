package com.jclinical.staff.infra.adapters.out;

import com.jclinical.core.security.StaffPermission;
import com.jclinical.staff.domain.model.StaffPermissionOverride;
import com.jclinical.staff.domain.model.StaffPermissionOverrideState;
import com.jclinical.staff.domain.ports.out.StaffPermissionOverrideRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SqlStaffPermissionOverrideRepository implements StaffPermissionOverrideRepositoryPort {

    private final SpringDataStaffPermissionOverrideRepository springDataRepository;

    @Override
    public StaffPermissionOverride save(StaffPermissionOverride override) {
        StaffPermissionOverrideEntity saved = springDataRepository.save(toEntity(override));
        return toDomain(saved);
    }

    @Override
    public List<StaffPermissionOverride> findByClinicIdAndStaffId(UUID clinicId, UUID staffId) {
        return springDataRepository.findByClinicIdAndStaffId(clinicId, staffId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<StaffPermissionOverride> findByClinicIdAndStaffIdAndPermission(UUID clinicId, UUID staffId, StaffPermission permission) {
        return springDataRepository.findByClinicIdAndStaffIdAndPermissionCode(clinicId, staffId, permission.name())
                .map(this::toDomain);
    }

    @Override
    public void delete(StaffPermissionOverride override) {
        springDataRepository.deleteById(override.getId());
    }

    private StaffPermissionOverrideEntity toEntity(StaffPermissionOverride domain) {
        return StaffPermissionOverrideEntity.builder()
                .id(domain.getId())
                .clinicId(domain.getClinicId())
                .staffId(domain.getStaffId())
                .permissionCode(domain.getPermission().name())
                .state(StaffPermissionOverrideEntity.OverrideState.valueOf(domain.getState().name()))
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    private StaffPermissionOverride toDomain(StaffPermissionOverrideEntity entity) {
        return StaffPermissionOverride.builder()
                .id(entity.getId())
                .clinicId(entity.getClinicId())
                .staffId(entity.getStaffId())
                .permission(StaffPermission.valueOf(entity.getPermissionCode()))
                .state(StaffPermissionOverrideState.valueOf(entity.getState().name()))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
