package com.jclinical.records.infra.adapters.in.web.dto;

import java.util.UUID;

public record SaveMedicalHistoryRequest(
    UUID clinicId,
    UUID templateId,
    String answersJson
) {}
