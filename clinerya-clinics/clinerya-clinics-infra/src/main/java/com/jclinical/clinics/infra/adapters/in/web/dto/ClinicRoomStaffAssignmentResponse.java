package com.jclinical.clinics.infra.adapters.in.web.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ClinicRoomStaffAssignmentResponse(
        UUID id,
        UUID clinicId,
        UUID roomId,
        UUID staffId,
        boolean active,
        LocalDateTime assignedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
