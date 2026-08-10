package com.jclinical.users.infra.adapters.in.web.dto;

public record RegisterUserRequest(
    String email,
    String password,
    String fullName,
    String clinicName
) {}
