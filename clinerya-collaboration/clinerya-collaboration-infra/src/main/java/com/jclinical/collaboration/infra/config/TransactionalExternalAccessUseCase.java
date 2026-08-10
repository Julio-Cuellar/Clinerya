package com.jclinical.collaboration.infra.config;

import com.jclinical.collaboration.domain.model.ExternalAccessGrant;
import com.jclinical.collaboration.domain.ports.in.ManageExternalAccessUseCase;
import com.jclinical.collaboration.domain.service.ExternalAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Primary
@RequiredArgsConstructor
public class TransactionalExternalAccessUseCase implements ManageExternalAccessUseCase {

    private final ExternalAccessService externalAccessService;

    @Override
    @Transactional
    public ExternalAccessGrant inviteExternalSpecialist(InviteCommand command) {
        return externalAccessService.inviteExternalSpecialist(command);
    }

    @Override
    @Transactional
    public ExternalAccessGrant acceptInvitation(UUID grantId, UUID respondingUserId, String respondingUserEmail) {
        return externalAccessService.acceptInvitation(grantId, respondingUserId, respondingUserEmail);
    }

    @Override
    @Transactional
    public ExternalAccessGrant rejectInvitation(UUID grantId, UUID respondingUserId, String respondingUserEmail) {
        return externalAccessService.rejectInvitation(grantId, respondingUserId, respondingUserEmail);
    }

    @Override
    @Transactional
    public void revokeAccess(UUID grantId, UUID sourceClinicId) {
        externalAccessService.revokeAccess(grantId, sourceClinicId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExternalAccessGrant> listGrantsBySourceClinic(UUID sourceClinicId) {
        return externalAccessService.listGrantsBySourceClinic(sourceClinicId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExternalAccessGrant> listGrantsForUser(UUID externalUserId, String externalUserEmail) {
        return externalAccessService.listGrantsForUser(externalUserId, externalUserEmail);
    }

    @Override
    @Transactional(readOnly = true)
    public AccessCheckResult checkAccess(UUID requestingUserId, UUID clinicId, UUID patientId) {
        return externalAccessService.checkAccess(requestingUserId, clinicId, patientId);
    }
}
