package com.jclinical.auth.infra.adapters.in.web.dto;

public record LoginRequest(
    String email,
    String password
) {}
