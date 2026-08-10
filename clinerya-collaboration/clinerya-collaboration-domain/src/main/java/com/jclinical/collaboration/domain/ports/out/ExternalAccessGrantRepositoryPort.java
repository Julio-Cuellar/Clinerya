package com.jclinical.collaboration.domain.ports.out;

import com.jclinical.collaboration.domain.model.ExternalAccessGrant;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExternalAccessGrantRepositoryPort {

    ExternalAccessGrant save(ExternalAccessGrant grant);

    Optional<ExternalAccessGrant> findById(UUID id);

    List<ExternalAccessGrant> findBySourceClinicId(UUID sourceClinicId);

    List<ExternalAccessGrant> findByExternalUserId(UUID externalUserId);

    Optional<ExternalAccessGrant> findActiveByExternalUserIdAndClinicIdAndPatientId(
            UUID externalUserId, UUID clinicId, UUID patientId);

    boolean existsActiveOrPendingByClinicIdAndPatientIdAndExternalUserId(
            UUID clinicId, UUID patientId, UUID externalUserId);

    boolean existsActiveOrPendingByClinicIdAndPatientIdAndEmail(
            UUID clinicId, UUID patientId, String invitedEmail);

    List<ExternalAccessGrant> findPendingByInvitedEmailIgnoreCaseAndExternalUserIdIsNull(String invitedEmail);
}
