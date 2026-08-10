package com.jclinical.collaboration.domain.ports.in;

import com.jclinical.collaboration.domain.model.AccessLevel;
import com.jclinical.collaboration.domain.model.ExternalAccessGrant;

import java.util.List;
import java.util.UUID;

public interface ManageExternalAccessUseCase {

    ExternalAccessGrant inviteExternalSpecialist(InviteCommand command);

    ExternalAccessGrant acceptInvitation(UUID grantId, UUID respondingUserId, String respondingUserEmail);

    ExternalAccessGrant rejectInvitation(UUID grantId, UUID respondingUserId, String respondingUserEmail);

    void revokeAccess(UUID grantId, UUID sourceClinicId);

    List<ExternalAccessGrant> listGrantsBySourceClinic(UUID sourceClinicId);

    List<ExternalAccessGrant> listGrantsForUser(UUID externalUserId, String externalUserEmail);

    /**
     * Consumido por otros módulos (ej. records) para saber si un usuario tiene acceso
     * externo activo a un paciente puntual de una clínica que no es la suya.
     */
    AccessCheckResult checkAccess(UUID requestingUserId, UUID clinicId, UUID patientId);

    record InviteCommand(
            UUID sourceClinicId,
            UUID invitingUserId,
            UUID patientId,
            String invitedEmail,
            AccessLevel accessLevel
    ) {}

    record AccessCheckResult(boolean granted, AccessLevel accessLevel) {
        public static AccessCheckResult none() {
            return new AccessCheckResult(false, null);
        }
    }
}
