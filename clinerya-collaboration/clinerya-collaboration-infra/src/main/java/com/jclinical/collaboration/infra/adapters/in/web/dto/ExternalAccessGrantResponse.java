package com.jclinical.collaboration.infra.adapters.in.web.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ExternalAccessGrantResponse(
        UUID id,
        UUID sourceClinicId,
        UUID patientId,
        String patientName,
        UUID invitedByStaffId,
        UUID externalUserId,
        String invitedEmail,
        String accessLevel,
        String status,
        LocalDateTime createdAt,
        LocalDateTime respondedAt,
        LocalDateTime revokedAt
) {}
