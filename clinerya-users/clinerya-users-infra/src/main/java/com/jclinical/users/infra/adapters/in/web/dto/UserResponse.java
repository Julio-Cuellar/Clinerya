package com.jclinical.users.infra.adapters.in.web.dto;

import java.util.UUID;

public record UserResponse(
    UUID id,
    String email,
    String fullName,
    String phone,
    String avatarUrl,
    boolean emailVerified,
    String themePreference,
    boolean active
) {}
