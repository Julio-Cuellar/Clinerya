package com.jclinical.core.notifications;

import java.time.LocalDateTime;
import java.util.UUID;

public interface NotificationPublisherPort {

    void publish(NotificationCommand command);

    record NotificationCommand(
            UUID clinicId,
            String sourceModule,
            String sourceKey,
            String severity,
            String title,
            String message,
            String actionPath,
            LocalDateTime expiresAt
    ) {}
}
