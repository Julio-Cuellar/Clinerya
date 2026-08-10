package com.jclinical.collaboration.infra.adapters.in.web;

import com.jclinical.collaboration.domain.model.AccessLevel;
import com.jclinical.collaboration.domain.model.ExternalAccessGrant;
import com.jclinical.collaboration.domain.ports.in.ManageExternalAccessUseCase;
import com.jclinical.collaboration.domain.ports.in.ManageExternalAccessUseCase.InviteCommand;
import com.jclinical.collaboration.domain.ports.out.PatientDirectoryPort;
import com.jclinical.collaboration.infra.adapters.in.web.dto.ExternalAccessGrantResponse;
import com.jclinical.collaboration.infra.adapters.in.web.dto.InviteExternalAccessRequest;
import com.jclinical.users.infra.security.CurrentUserResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ExternalAccessController {

    private final ManageExternalAccessUseCase externalAccessUseCase;
    private final PatientDirectoryPort patientDirectory;
    private final CurrentUserResolver currentUserResolver;

    @PostMapping("/api/v1/clinics/{clinicId}/patients/{patientId}/external-access")
    public ResponseEntity<ExternalAccessGrantResponse> invite(
            @PathVariable UUID clinicId,
            @PathVariable UUID patientId,
            @RequestBody InviteExternalAccessRequest request) {
        InviteCommand command = new InviteCommand(
                clinicId,
                currentUserResolver.getCurrentUserId(),
                patientId,
                request.email(),
                AccessLevel.valueOf(request.accessLevel())
        );
        ExternalAccessGrant grant = externalAccessUseCase.inviteExternalSpecialist(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(grant));
    }

    @GetMapping("/api/v1/clinics/{clinicId}/external-access")
    public ResponseEntity<List<ExternalAccessGrantResponse>> listByClinic(@PathVariable UUID clinicId) {
        List<ExternalAccessGrantResponse> responses = externalAccessUseCase.listGrantsBySourceClinic(clinicId).stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/api/v1/clinics/{clinicId}/external-access/{grantId}")
    public ResponseEntity<Void> revoke(@PathVariable UUID clinicId, @PathVariable UUID grantId) {
        externalAccessUseCase.revokeAccess(grantId, clinicId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/me/external-access")
    public ResponseEntity<List<ExternalAccessGrantResponse>> listMine() {
        UUID userId = currentUserResolver.getCurrentUserId();
        String email = currentUserResolver.getCurrentUserEmail();
        List<ExternalAccessGrantResponse> responses = externalAccessUseCase.listGrantsForUser(userId, email).stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/api/v1/me/external-access/{grantId}/accept")
    public ResponseEntity<ExternalAccessGrantResponse> accept(@PathVariable UUID grantId) {
        ExternalAccessGrant grant = externalAccessUseCase.acceptInvitation(
                grantId, currentUserResolver.getCurrentUserId(), currentUserResolver.getCurrentUserEmail());
        return ResponseEntity.ok(toResponse(grant));
    }

    @PostMapping("/api/v1/me/external-access/{grantId}/reject")
    public ResponseEntity<ExternalAccessGrantResponse> reject(@PathVariable UUID grantId) {
        ExternalAccessGrant grant = externalAccessUseCase.rejectInvitation(
                grantId, currentUserResolver.getCurrentUserId(), currentUserResolver.getCurrentUserEmail());
        return ResponseEntity.ok(toResponse(grant));
    }

    private ExternalAccessGrantResponse toResponse(ExternalAccessGrant grant) {
        String patientName = patientDirectory.findPatient(grant.getPatientId(), grant.getSourceClinicId())
                .map(PatientDirectoryPort.PatientSummary::fullName)
                .orElse(null);
        return new ExternalAccessGrantResponse(
                grant.getId(),
                grant.getSourceClinicId(),
                grant.getPatientId(),
                patientName,
                grant.getInvitedByStaffId(),
                grant.getExternalUserId(),
                grant.getInvitedEmail(),
                grant.getAccessLevel().name(),
                grant.getStatus().name(),
                grant.getCreatedAt(),
                grant.getRespondedAt(),
                grant.getRevokedAt()
        );
    }
}
