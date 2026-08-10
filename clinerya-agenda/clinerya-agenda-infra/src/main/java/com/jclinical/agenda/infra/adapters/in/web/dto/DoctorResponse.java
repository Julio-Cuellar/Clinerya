package com.jclinical.agenda.infra.adapters.in.web.dto;

import java.util.UUID;

public record DoctorResponse(
        UUID staffId,
        String fullName
) {}
