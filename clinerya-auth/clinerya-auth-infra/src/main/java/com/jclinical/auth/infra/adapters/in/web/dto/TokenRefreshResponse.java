package com.jclinical.auth.infra.adapters.in.web.dto;

public record TokenRefreshResponse(
    String token,
    String refreshToken,
    String tokenType
) {}
