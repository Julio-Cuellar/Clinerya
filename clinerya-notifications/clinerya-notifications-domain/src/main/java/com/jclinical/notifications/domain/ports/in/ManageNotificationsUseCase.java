package com.jclinical.notifications.domain.ports.in;

import com.jclinical.notifications.domain.model.Notification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ManageNotificationsUseCase {

    Notification publish(PublishNotificationCommand command);

    NotificationList list(UUID clinicId, boolean unreadOnly, int limit);

    Notification markRead(UUID clinicId, UUID notificationId);

    void markAllRead(UUID clinicId);

    record PublishNotificationCommand(
            UUID clinicId,
            String sourceModule,
            String sourceKey,
            String severity,
            String title,
            String message,
            String actionPath,
            LocalDateTime expiresAt
    ) {}

    record NotificationList(List<Notification> items, long unreadCount) {}
}
