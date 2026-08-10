package com.jclinical.records.infra.adapters.in.web.dto;

public record CreateHistoryTemplateRequest(
    String name,
    String description,
    String schemaJson
) {}
