package com.jclinical.notifications.infra.config;

import com.jclinical.core.notifications.NotificationPublisherPort;
import com.jclinical.notifications.domain.model.Notification;
import com.jclinical.notifications.domain.ports.in.ManageNotificationsUseCase;
import com.jclinical.notifications.domain.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Primary
@RequiredArgsConstructor
public class TransactionalNotificationsUseCase implements ManageNotificationsUseCase, NotificationPublisherPort {

    private final NotificationService notificationService;

    @Override
    @Transactional
    public Notification publish(PublishNotificationCommand command) {
        return notificationService.publish(command);
    }

    @Override
    @Transactional
    public void publish(NotificationCommand command) {
        notificationService.publish(new PublishNotificationCommand(
                command.clinicId(),
                command.sourceModule(),
                command.sourceKey(),
                command.severity(),
                command.title(),
                command.message(),
                command.actionPath(),
                command.expiresAt()
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationList list(UUID clinicId, UUID actingUserId, boolean unreadOnly, int limit) {
        return notificationService.list(clinicId, actingUserId, unreadOnly, limit);
    }

    @Override
    @Transactional
    public Notification markRead(UUID clinicId, UUID actingUserId, UUID notificationId) {
        return notificationService.markRead(clinicId, actingUserId, notificationId);
    }

    @Override
    @Transactional
    public void markAllRead(UUID clinicId, UUID actingUserId) {
        notificationService.markAllRead(clinicId, actingUserId);
    }
}
