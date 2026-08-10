package com.jclinical.auth.infra.adapters.in.web.dto;

import java.util.UUID;

public record AuthUserResponse(
    UUID id,
    String email,
    String fullName,
    String phone,
    String avatarUrl,
    boolean emailVerified,
    String themePreference,
    boolean active
) {}
