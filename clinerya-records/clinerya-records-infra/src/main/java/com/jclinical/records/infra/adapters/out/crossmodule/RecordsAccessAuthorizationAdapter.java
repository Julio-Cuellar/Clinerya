package com.jclinical.records.infra.adapters.out.crossmodule;

import com.jclinical.staff.domain.ports.in.ManageClinicStaffUseCase;
import com.jclinical.collaboration.domain.ports.in.ManageExternalAccessUseCase;
import com.jclinical.collaboration.domain.ports.in.ManageExternalAccessUseCase.AccessCheckResult;
import com.jclinical.records.domain.ports.out.PatientAccessAuthorizationPort;
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
            if (staff.role() == com.jclinical.staff.domain.model.StaffRole.RECEPTIONIST
                    || staff.role() == com.jclinical.staff.domain.model.StaffRole.ACCOUNTANT
                    || staff.role() == com.jclinical.staff.domain.model.StaffRole.CLEANING) {
                return new AccessDecision(AccessLevel.NONE, false);
            }
            if (staff.role() == com.jclinical.staff.domain.model.StaffRole.ASSISTANT) {
                return new AccessDecision(AccessLevel.READ_ONLY, false);
            }
            return new AccessDecision(AccessLevel.READ_WRITE, false);
        }

        AccessCheckResult result = externalAccessUseCase.checkAccess(requestingUserId, clinicId, patientId);
        if (!result.granted()) {
            return new AccessDecision(AccessLevel.NONE, false);
        }
        return new AccessDecision(toRecordsLevel(result.accessLevel()), true);
    }

    private AccessLevel toRecordsLevel(com.jclinical.collaboration.domain.model.AccessLevel grantLevel) {
        return switch (grantLevel) {
            case READ_ONLY -> AccessLevel.READ_ONLY;
            case COMMENT -> AccessLevel.COMMENT;
            case FULL -> AccessLevel.READ_WRITE;
        };
    }
}
