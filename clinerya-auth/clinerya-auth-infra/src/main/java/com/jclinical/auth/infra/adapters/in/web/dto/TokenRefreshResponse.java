package com.jclinical.auth.infra.adapters.in.web.dto;

/** El refresh token no cambia en cada renovacion: sigue viviendo solo en la cookie HttpOnly. */
public record TokenRefreshResponse(
    String token,
    String tokenType
) {}
