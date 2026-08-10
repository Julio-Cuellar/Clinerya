package com.jclinical.notifications.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    private UUID id;
    private UUID clinicId;
    private String sourceModule;
    private String sourceKey;
    private String severity;
    private String title;
    private String message;
    private String actionPath;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private LocalDateTime readAt;
}
