package com.jclinical.staff.domain.ports.in;

import com.jclinical.staff.domain.model.StaffRole;
import com.jclinical.staff.domain.model.StaffPermission;
import com.jclinical.staff.domain.model.StaffPermissionOverrideState;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ManageClinicStaffUseCase {

    List<StaffSummary> listStaffByClinic(UUID clinicId, StaffRole role);

    Optional<StaffSummary> getActiveStaffById(UUID staffId, UUID clinicId);

    Optional<StaffSummary> getActiveStaffByUserAndClinic(UUID userId, UUID clinicId);

    StaffSummary addStaff(UUID clinicId, String email, StaffRole role);

    StaffInvitationSummary inviteStaff(UUID clinicId, String email, StaffRole role);

    List<StaffInvitationSummary> listInvitations(UUID clinicId);

    StaffSummary updateStaff(UUID clinicId, UUID staffId, StaffRole role);

    PermissionSummary getPermissions(UUID clinicId, UUID staffId);

    PermissionSummary updatePermissions(UUID clinicId, UUID staffId, List<PermissionChange> changes);

    void removeStaff(UUID clinicId, UUID staffId);

    record PermissionChange(
            StaffPermission permission,
            StaffPermissionOverrideState state
    ) {}

    record PermissionItem(
            StaffPermission permission,
            StaffPermissionOverrideState overrideState,
            boolean enabled
    ) {}

    record PermissionSummary(
            UUID staffId,
            StaffRole role,
            List<PermissionItem> permissions
    ) {}

    record StaffInvitationSummary(
            UUID invitationId,
            UUID clinicId,
            String email,
            StaffRole role,
            String token,
            boolean used,
            LocalDateTime expiresAt
    ) {}

    record StaffSummary(
            UUID staffId,
            UUID clinicId,
            UUID userId,
            StaffRole role,
            String fullName
    ) {}
}
