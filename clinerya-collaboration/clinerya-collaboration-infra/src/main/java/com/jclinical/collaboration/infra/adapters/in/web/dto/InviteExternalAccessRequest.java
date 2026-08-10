package com.jclinical.collaboration.infra.adapters.in.web.dto;

public record InviteExternalAccessRequest(
        String email,
        String accessLevel
) {}
