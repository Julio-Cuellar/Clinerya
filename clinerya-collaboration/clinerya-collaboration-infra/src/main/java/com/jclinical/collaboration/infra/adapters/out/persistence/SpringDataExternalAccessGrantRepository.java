package com.jclinical.collaboration.infra.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataExternalAccessGrantRepository extends JpaRepository<ExternalAccessGrantEntity, UUID> {

    List<ExternalAccessGrantEntity> findBySourceClinicId(UUID sourceClinicId);

    List<ExternalAccessGrantEntity> findByExternalUserId(UUID externalUserId);

    Optional<ExternalAccessGrantEntity> findByExternalUserIdAndSourceClinicIdAndPatientIdAndStatus(
            UUID externalUserId, UUID sourceClinicId, UUID patientId, String status);

    boolean existsBySourceClinicIdAndPatientIdAndExternalUserIdAndStatusIn(
            UUID sourceClinicId, UUID patientId, UUID externalUserId, List<String> statuses);

    boolean existsBySourceClinicIdAndPatientIdAndInvitedEmailIgnoreCaseAndStatusIn(
            UUID sourceClinicId, UUID patientId, String invitedEmail, List<String> statuses);

    List<ExternalAccessGrantEntity> findByInvitedEmailIgnoreCaseAndExternalUserIdIsNullAndStatus(
            String invitedEmail, String status);
}
