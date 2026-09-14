package com.jclinical.records.infra.adapters.in.web.dto;

import com.jclinical.records.domain.model.Icd10Code;

public record Icd10CodeResponse(
    String code,
    String description,
    String chapter,
    boolean billable
) {
    public static Icd10CodeResponse from(Icd10Code code) {
        return new Icd10CodeResponse(code.getCode(), code.getDescription(), code.getChapter(), code.isBillable());
    }
}
