package com.jclinical.clinics.infra.adapters.in.web.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ClinicRoomResponse(
        UUID id,
        UUID clinicId,
        String name,
        String code,
        String colorHex,
        String description,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
