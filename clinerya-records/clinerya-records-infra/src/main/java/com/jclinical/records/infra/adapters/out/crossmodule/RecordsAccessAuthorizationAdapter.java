package com.jclinical.records.infra.adapters.out.crossmodule;

import com.jclinical.core.security.StaffPermission;
import com.jclinical.staff.domain.ports.in.ManageClinicStaffUseCase;
import com.jclinical.staff.domain.ports.in.ManageClinicStaffUseCase.PermissionSummary;
import com.jclinical.collaboration.domain.ports.in.ManageExternalAccessUseCase;
import com.jclinical.collaboration.domain.ports.in.ManageExternalAccessUseCase.AccessCheckResult;
import com.jclinical.core.security.PatientAccessAuthorizationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RecordsAccessAuthorizationAdapter implements PatientAccessAuthorizationPort {

    private final ManageClinicStaffUseCase clinicStaffUseCase;
    private final ManageExternalAccessUseCase externalAccessUseCase;

    @Override
    public AccessDecision resolveAccess(UUID requestingUserId, UUID clinicId, UUID patientId) {
        var staffOpt = clinicStaffUseCase.getActiveStaffByUserAndClinic(requestingUserId, clinicId);
        if (staffOpt.isPresent()) {
            var staff = staffOpt.get();
            // Antes se decidia solo por StaffRole, asi que quitarle VIEW_MEDICAL_RECORDS a
            // un doctor via override no le quitaba el acceso: el rol seguia siendo DOCTOR.
            // getPermissions() ya combina el rol con los overrides individuales.
            PermissionSummary summary = clinicStaffUseCase.getPermissions(clinicId, staff.staffId());
            boolean canEdit = hasPermission(summary, StaffPermission.EDIT_MEDICAL_RECORDS);
            boolean canView = canEdit || hasPermission(summary, StaffPermission.VIEW_MEDICAL_RECORDS);

            if (canEdit) {
                return new AccessDecision(AccessLevel.READ_WRITE, false);
            }
            if (canView) {
                return new AccessDecision(AccessLevel.READ_ONLY, false);
            }
            return new AccessDecision(AccessLevel.NONE, false);
        }

        AccessCheckResult result = externalAccessUseCase.checkAccess(requestingUserId, clinicId, patientId);
        if (!result.granted()) {
            return new AccessDecision(AccessLevel.NONE, false);
        }
        return new AccessDecision(toRecordsLevel(result.accessLevel()), true);
    }

    private boolean hasPermission(PermissionSummary summary, StaffPermission permission) {
        return summary.permissions().stream()
                .anyMatch(item -> item.permission() == permission && item.enabled());
    }

    private AccessLevel toRecordsLevel(com.jclinical.collaboration.domain.model.AccessLevel grantLevel) {
        return switch (grantLevel) {
            case READ_ONLY -> AccessLevel.READ_ONLY;
            case COMMENT -> AccessLevel.COMMENT;
            case FULL -> AccessLevel.READ_WRITE;
        };
    }
}
