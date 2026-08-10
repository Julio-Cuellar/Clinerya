package com.jclinical.collaboration.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalAccessGrant {
    private UUID id;
    private UUID sourceClinicId;
    private UUID patientId;
    private UUID invitedByStaffId;
    private UUID externalUserId;
    private String invitedEmail;
    private AccessLevel accessLevel;
    private ExternalAccessStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;
    private LocalDateTime revokedAt;

    public void accept() {
        if (status != ExternalAccessStatus.PENDING) {
            throw new IllegalStateException("Solo se puede aceptar una invitación pendiente.");
        }
        this.status = ExternalAccessStatus.ACCEPTED;
        this.respondedAt = LocalDateTime.now();
    }

    public void reject() {
        if (status != ExternalAccessStatus.PENDING) {
            throw new IllegalStateException("Solo se puede rechazar una invitación pendiente.");
        }
        this.status = ExternalAccessStatus.REJECTED;
        this.respondedAt = LocalDateTime.now();
    }

    public void revoke() {
        if (status != ExternalAccessStatus.PENDING && status != ExternalAccessStatus.ACCEPTED) {
            throw new IllegalStateException("Solo se puede revocar un acceso pendiente o activo.");
        }
        this.status = ExternalAccessStatus.REVOKED;
        this.revokedAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return status == ExternalAccessStatus.ACCEPTED;
    }

    public boolean matchesEmail(String email) {
        return invitedEmail != null && invitedEmail.equalsIgnoreCase(email);
    }

    public void bindExternalUser(UUID userId) {
        this.externalUserId = userId;
    }
}
