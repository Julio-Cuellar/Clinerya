package com.jclinical.notifications.infra.adapters.in.web.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String sourceModule,
        String sourceKey,
        String severity,
        String title,
        String message,
        String actionPath,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        LocalDateTime readAt
) {}
