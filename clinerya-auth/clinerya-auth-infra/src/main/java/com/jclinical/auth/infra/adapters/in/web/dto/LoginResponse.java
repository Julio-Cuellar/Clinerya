package com.jclinical.auth.infra.adapters.in.web.dto;

public record LoginResponse(
    String token,
    String refreshToken,
    String tokenType,
    AuthUserResponse user
) {}
