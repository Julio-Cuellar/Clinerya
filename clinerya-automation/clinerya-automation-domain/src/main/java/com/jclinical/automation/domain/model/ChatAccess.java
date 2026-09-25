package com.jclinical.automation.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

/** Registro de auditoria: quien leyo que chat y cuando. */
public record ChatAccess(UUID id, UUID clinicId, String phone, UUID userId, LocalDateTime accessedAt) {}
