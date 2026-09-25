package com.jclinical.users.infra.adapters.in.web.dto;

import java.util.List;
import java.util.UUID;

public record UserMeResponse(
    UUID id,
    String email,
    String fullName,
    String phone,
    String avatarUrl,
    boolean emailVerified,
    String themePreference,
    boolean active,
    List<ClinicDto> clinics
) {
    public record ClinicDto(
        UUID id,
        String name,
        String role,
        List<String> permissions,
        boolean attendsPatients
    ) {}
}
