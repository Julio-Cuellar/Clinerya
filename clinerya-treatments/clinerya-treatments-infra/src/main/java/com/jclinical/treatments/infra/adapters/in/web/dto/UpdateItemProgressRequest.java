package com.jclinical.treatments.infra.adapters.in.web.dto;

import com.jclinical.treatments.domain.model.ItemProgressStatus;

import java.util.UUID;

public record UpdateItemProgressRequest(
    UUID clinicId,
    ItemProgressStatus progressStatus
) {}
