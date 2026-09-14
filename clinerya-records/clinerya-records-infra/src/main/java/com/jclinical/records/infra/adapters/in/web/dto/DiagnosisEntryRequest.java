package com.jclinical.records.infra.adapters.in.web.dto;

import com.jclinical.records.domain.model.DiagnosisKind;

public record DiagnosisEntryRequest(
    String icd10Code,
    DiagnosisKind kind
) {}
