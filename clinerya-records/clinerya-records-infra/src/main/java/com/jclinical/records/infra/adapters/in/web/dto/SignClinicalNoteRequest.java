package com.jclinical.records.infra.adapters.in.web.dto;

import java.util.List;

public record SignClinicalNoteRequest(
    List<DiagnosisEntryRequest> diagnoses
) {}
