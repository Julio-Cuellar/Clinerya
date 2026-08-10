package com.jclinical.records.infra.adapters.in.web.dto;

public record UpdateHistoryTemplateRequest(
    String name,
    String description,
    String schemaJson,
    boolean active
) {}
