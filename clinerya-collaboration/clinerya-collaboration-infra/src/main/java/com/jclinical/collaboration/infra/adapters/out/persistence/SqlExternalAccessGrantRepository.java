package com.jclinical.collaboration.infra.adapters.out.persistence;

import com.jclinical.collaboration.domain.model.ExternalAccessGrant;
import com.jclinical.collaboration.domain.model.ExternalAccessStatus;
import com.jclinical.collaboration.domain.ports.out.ExternalAccessGrantRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SqlExternalAccessGrantRepository implements ExternalAccessGrantRepositoryPort {

    private static final List<String> PENDING_OR_ACTIVE_STATUSES = List.of(
            ExternalAccessStatus.PENDING.name(), ExternalAccessStatus.ACCEPTED.name());

    private final SpringDataExternalAccessGrantRepository springDataRepository;
    private final ExternalAccessGrantMapper mapper;

    @Override
    public ExternalAccessGrant save(ExternalAccessGrant grant) {
        ExternalAccessGrantEntity entity = mapper.toEntity(grant);
        return mapper.toDomain(springDataRepository.save(entity));
    }

    @Override
    public Optional<ExternalAccessGrant> findById(UUID id) {
        return springDataRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<ExternalAccessGrant> findBySourceClinicId(UUID sourceClinicId) {
        return springDataRepository.findBySourceClinicId(sourceClinicId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<ExternalAccessGrant> findByExternalUserId(UUID externalUserId) {
        return springDataRepository.findByExternalUserId(externalUserId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<ExternalAccessGrant> findActiveByExternalUserIdAndClinicIdAndPatientId(
            UUID externalUserId, UUID clinicId, UUID patientId) {
        return springDataRepository.findByExternalUserIdAndSourceClinicIdAndPatientIdAndStatus(
                        externalUserId, clinicId, patientId, ExternalAccessStatus.ACCEPTED.name())
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsActiveOrPendingByClinicIdAndPatientIdAndExternalUserId(
            UUID clinicId, UUID patientId, UUID externalUserId) {
        return springDataRepository.existsBySourceClinicIdAndPatientIdAndExternalUserIdAndStatusIn(
                clinicId, patientId, externalUserId, PENDING_OR_ACTIVE_STATUSES);
    }

    @Override
    public boolean existsActiveOrPendingByClinicIdAndPatientIdAndEmail(
            UUID clinicId, UUID patientId, String invitedEmail) {
        return springDataRepository.existsBySourceClinicIdAndPatientIdAndInvitedEmailIgnoreCaseAndStatusIn(
                clinicId, patientId, invitedEmail, PENDING_OR_ACTIVE_STATUSES);
    }

    @Override
    public List<ExternalAccessGrant> findPendingByInvitedEmailIgnoreCaseAndExternalUserIdIsNull(String invitedEmail) {
        return springDataRepository.findByInvitedEmailIgnoreCaseAndExternalUserIdIsNullAndStatus(
                        invitedEmail, ExternalAccessStatus.PENDING.name())
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
