package com.jclinical.staff.domain.ports.in;

import com.jclinical.staff.domain.model.StaffRole;
import com.jclinical.core.security.StaffPermission;
import com.jclinical.staff.domain.model.StaffPermissionOverrideState;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ManageClinicStaffUseCase {

    List<StaffSummary> listStaffByClinic(UUID clinicId, StaffRole role);

    /** Personal activo que atiende pacientes: doctores y administradores con la bandera activa. */
    List<StaffSummary> listPractitioners(UUID clinicId);

    Optional<StaffSummary> getActiveStaffById(UUID staffId, UUID clinicId);

    Optional<StaffSummary> getActiveStaffByUserAndClinic(UUID userId, UUID clinicId);

    StaffSummary addStaff(UUID clinicId, String email, StaffRole role);

    StaffInvitationSummary inviteStaff(UUID clinicId, String email, StaffRole role);

    List<StaffInvitationSummary> listInvitations(UUID clinicId);

    StaffSummary updateStaff(UUID clinicId, UUID staffId, StaffRole role);

    PermissionSummary getPermissions(UUID clinicId, UUID staffId);

    PermissionSummary updatePermissions(UUID clinicId, UUID staffId, List<PermissionChange> changes);

    ClinicalPracticeSummary getClinicalPractice(UUID clinicId, UUID staffId);

    /**
     * Marca si un administrador atiende pacientes. Activarlo exige cedula profesional, y es lo
     * unico que le da acceso clinico: sus permisos clinicos no se pueden otorgar a mano.
     */
    ClinicalPracticeSummary updateClinicalPractice(UUID clinicId, UUID staffId, boolean attendsPatients,
                                                   String cedulaProfesional);

    void removeStaff(UUID clinicId, UUID staffId);

    record PermissionChange(
            StaffPermission permission,
            StaffPermissionOverrideState state
    ) {}

    /** {@code locked}: el permiso no admite override (superadministrador, o clinico en un rol administrativo). */
    record PermissionItem(
            StaffPermission permission,
            StaffPermissionOverrideState overrideState,
            boolean enabled,
            boolean locked
    ) {
        public PermissionItem(StaffPermission permission, StaffPermissionOverrideState overrideState, boolean enabled) {
            this(permission, overrideState, enabled, false);
        }
    }

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

    /** {@code practitioner}: atiende pacientes, sea por ser DOCTOR o por ser administrador con la bandera activa. */
    record StaffSummary(
            UUID staffId,
            UUID clinicId,
            UUID userId,
            StaffRole role,
            String fullName,
            boolean attendsPatients,
            boolean practitioner
    ) {
        public StaffSummary(UUID staffId, UUID clinicId, UUID userId, StaffRole role, String fullName) {
            this(staffId, clinicId, userId, role, fullName, false, role == StaffRole.DOCTOR);
        }
    }

    record ClinicalPracticeSummary(
            UUID staffId,
            StaffRole role,
            boolean attendsPatients,
            boolean practitioner,
            String cedulaProfesional,
            String credentialStatus
    ) {}
}
