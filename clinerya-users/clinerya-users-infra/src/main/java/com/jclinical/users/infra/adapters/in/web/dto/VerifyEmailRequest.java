package com.jclinical.users.infra.adapters.in.web.dto;

public record VerifyEmailRequest(
    String email,
    String token
) {}
