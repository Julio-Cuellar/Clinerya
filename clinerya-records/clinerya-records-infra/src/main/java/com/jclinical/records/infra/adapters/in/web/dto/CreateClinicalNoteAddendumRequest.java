package com.jclinical.records.infra.adapters.in.web.dto;

import java.util.UUID;

public record CreateClinicalNoteAddendumRequest(
    UUID clinicId,
    String content
) {}
