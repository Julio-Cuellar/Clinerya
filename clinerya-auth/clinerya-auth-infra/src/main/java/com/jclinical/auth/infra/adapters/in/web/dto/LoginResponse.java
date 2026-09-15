package com.jclinical.auth.infra.adapters.in.web.dto;

/**
 * El refresh token ya no viaja aqui: se entrega como cookie HttpOnly (ver
 * {@code AuthController}), para que un XSS que lea localStorage no pueda robarlo.
 */
public record LoginResponse(
    String token,
    String tokenType,
    AuthUserResponse user
) {}
