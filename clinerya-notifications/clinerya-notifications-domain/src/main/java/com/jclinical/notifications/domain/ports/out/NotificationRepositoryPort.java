package com.jclinical.notifications.domain.ports.out;

import com.jclinical.notifications.domain.model.Notification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepositoryPort {

    Notification save(Notification notification);

    Optional<Notification> findByClinicIdAndId(UUID clinicId, UUID notificationId);

    Optional<Notification> findByClinicIdAndSourceKey(UUID clinicId, String sourceModule, String sourceKey);

    List<Notification> findByClinicId(UUID clinicId, boolean unreadOnly, int limit);

    long countUnreadByClinicId(UUID clinicId);

    void markAllRead(UUID clinicId);
}
