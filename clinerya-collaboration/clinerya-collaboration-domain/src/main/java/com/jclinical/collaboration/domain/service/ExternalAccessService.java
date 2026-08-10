package com.jclinical.collaboration.domain.service;

import com.jclinical.collaboration.domain.model.ExternalAccessGrant;
import com.jclinical.collaboration.domain.model.ExternalAccessStatus;
import com.jclinical.collaboration.domain.ports.in.ManageExternalAccessUseCase;
import com.jclinical.collaboration.domain.ports.out.ClinicStaffDirectoryPort;
import com.jclinical.collaboration.domain.ports.out.ExternalAccessGrantRepositoryPort;
import com.jclinical.collaboration.domain.ports.out.PatientDirectoryPort;
import com.jclinical.collaboration.domain.ports.out.UserDirectoryPort;
import com.jclinical.core.security.ClinicAccessDeniedException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ExternalAccessService implements ManageExternalAccessUseCase {

    private final ExternalAccessGrantRepositoryPort repository;
    private final ClinicStaffDirectoryPort staffDirectory;
    private final UserDirectoryPort userDirectory;
    private final PatientDirectoryPort patientDirectory;

    public ExternalAccessService(
            ExternalAccessGrantRepositoryPort repository,
            ClinicStaffDirectoryPort staffDirectory,
            UserDirectoryPort userDirectory,
            PatientDirectoryPort patientDirectory) {
        this.repository = repository;
        this.staffDirectory = staffDirectory;
        this.userDirectory = userDirectory;
        this.patientDirectory = patientDirectory;
    }

    @Override
    public ExternalAccessGrant inviteExternalSpecialist(InviteCommand command) {
        UUID invitingStaffId = staffDirectory.findActiveStaffId(command.invitingUserId(), command.sourceClinicId())
                .orElseThrow(() -> new ClinicAccessDeniedException(
                        "Solo el personal activo de la clínica puede invitar especialistas externos."));

        patientDirectory.findPatient(command.patientId(), command.sourceClinicId())
                .orElseThrow(() -> new IllegalArgumentException("El paciente no existe en esta clínica."));

        Optional<UserDirectoryPort.UserSummary> invitedUser = userDirectory.findByEmail(command.invitedEmail());

        UUID invitedUserId = null;
        if (invitedUser.isPresent()) {
            invitedUserId = invitedUser.get().id();
            if (staffDirectory.findActiveStaffId(invitedUserId, command.sourceClinicId()).isPresent()) {
                throw new IllegalArgumentException("El usuario ya es parte del personal de esta clínica.");
            }
            if (repository.existsActiveOrPendingByClinicIdAndPatientIdAndExternalUserId(
                    command.sourceClinicId(), command.patientId(), invitedUserId)) {
                throw new IllegalArgumentException(
                        "Ya existe una invitación pendiente o activa para este especialista y este paciente.");
            }
        } else if (repository.existsActiveOrPendingByClinicIdAndPatientIdAndEmail(
                command.sourceClinicId(), command.patientId(), command.invitedEmail())) {
            throw new IllegalArgumentException(
                    "Ya existe una invitación pendiente o activa para este correo y este paciente.");
        }

        ExternalAccessGrant grant = ExternalAccessGrant.builder()
                .id(UUID.randomUUID())
                .sourceClinicId(command.sourceClinicId())
                .patientId(command.patientId())
                .invitedByStaffId(invitingStaffId)
                .externalUserId(invitedUserId)
                .invitedEmail(command.invitedEmail())
                .accessLevel(command.accessLevel())
                .status(ExternalAccessStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        System.out.println(">>>> [ACCESO-EXTERNO] Enlace de invitación generado para '" + command.invitedEmail() + "': http://localhost:5173/accept-sharing?grantId=" + grant.getId());

        return repository.save(grant);
    }

    @Override
    public ExternalAccessGrant acceptInvitation(UUID grantId, UUID respondingUserId, String respondingUserEmail) {
        ExternalAccessGrant grant = loadOwnedByUser(grantId, respondingUserId, respondingUserEmail);
        grant.accept();
        return repository.save(grant);
    }

    @Override
    public ExternalAccessGrant rejectInvitation(UUID grantId, UUID respondingUserId, String respondingUserEmail) {
        ExternalAccessGrant grant = loadOwnedByUser(grantId, respondingUserId, respondingUserEmail);
        grant.reject();
        return repository.save(grant);
    }

    @Override
    public void revokeAccess(UUID grantId, UUID sourceClinicId) {
        ExternalAccessGrant grant = repository.findById(grantId)
                .orElseThrow(() -> new IllegalArgumentException("El acceso no existe."));
        if (!grant.getSourceClinicId().equals(sourceClinicId)) {
            throw new ClinicAccessDeniedException("Este acceso no pertenece a tu clínica.");
        }
        grant.revoke();
        repository.save(grant);
    }

    @Override
    public List<ExternalAccessGrant> listGrantsBySourceClinic(UUID sourceClinicId) {
        return repository.findBySourceClinicId(sourceClinicId);
    }

    @Override
    public List<ExternalAccessGrant> listGrantsForUser(UUID externalUserId, String externalUserEmail) {
        List<ExternalAccessGrant> grants = new ArrayList<>(repository.findByExternalUserId(externalUserId));
        grants.addAll(repository.findPendingByInvitedEmailIgnoreCaseAndExternalUserIdIsNull(externalUserEmail));
        return grants;
    }

    @Override
    public AccessCheckResult checkAccess(UUID requestingUserId, UUID clinicId, UUID patientId) {
        return repository.findActiveByExternalUserIdAndClinicIdAndPatientId(requestingUserId, clinicId, patientId)
                .map(grant -> new AccessCheckResult(true, grant.getAccessLevel()))
                .orElse(AccessCheckResult.none());
    }

    private ExternalAccessGrant loadOwnedByUser(UUID grantId, UUID userId, String userEmail) {
        ExternalAccessGrant grant = repository.findById(grantId)
                .orElseThrow(() -> new IllegalArgumentException("La invitación no existe."));
        if (grant.getExternalUserId() != null) {
            if (!grant.getExternalUserId().equals(userId)) {
                throw new ClinicAccessDeniedException("Esta invitación no te pertenece.");
            }
        } else if (grant.matchesEmail(userEmail)) {
            grant.bindExternalUser(userId);
        } else {
            throw new ClinicAccessDeniedException("Esta invitación no te pertenece.");
        }
        return grant;
    }
}
