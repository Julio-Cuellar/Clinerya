package com.jclinical.collaboration.infra.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "external_access_grants", schema = "collaboration")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalAccessGrantEntity {

    @Id
    private UUID id;

    @Column(name = "source_clinic_id", nullable = false)
    private UUID sourceClinicId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "invited_by_staff_id", nullable = false)
    private UUID invitedByStaffId;

    @Column(name = "external_user_id")
    private UUID externalUserId;

    @Column(name = "invited_email", nullable = false)
    private String invitedEmail;

    @Column(name = "access_level", nullable = false)
    private String accessLevel;

    @Column(nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;
}
