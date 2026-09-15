package com.jclinical.staff.infra.adapters.out;

import com.jclinical.staff.domain.model.ClinicStaff;
import com.jclinical.core.security.StaffPermission;
import com.jclinical.staff.domain.ports.in.ManageClinicStaffUseCase;
import com.jclinical.staff.domain.ports.out.ClinicStaffRepositoryPort;
import com.jclinical.core.security.StaffPermissionCheckerPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Resuelve permisos operativos reutilizando el modelo de roles y overrides que ya
 * expone {@link ManageClinicStaffUseCase#getPermissions}. El usuario se traduce a
 * su registro de personal activo dentro de la clinica antes de evaluar el permiso.
 */
@Component
@RequiredArgsConstructor
public class StaffPermissionCheckerAdapter implements StaffPermissionCheckerPort {

    private final ClinicStaffRepositoryPort clinicStaffRepository;
    private final ManageClinicStaffUseCase clinicStaffUseCase;

    @Override
    public boolean hasPermission(UUID clinicId, UUID userId, StaffPermission permission) {
        if (clinicId == null || userId == null || permission == null) {
            return false;
        }
        return clinicStaffRepository.findByClinicIdAndUserId(clinicId, userId)
                .filter(ClinicStaff::isActive)
                .map(staff -> clinicStaffUseCase.getPermissions(clinicId, staff.getId()))
                .map(summary -> summary.permissions().stream()
                        .anyMatch(item -> item.permission() == permission && item.enabled()))
                .orElse(false);
    }
}
