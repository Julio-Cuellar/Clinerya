package com.jclinical.notifications.infra.adapters.out.persistence;

import com.jclinical.notifications.domain.model.Notification;
import com.jclinical.notifications.domain.ports.out.NotificationRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SqlNotificationRepository implements NotificationRepositoryPort {

    private final SpringDataNotificationRepository repository;

    @Override
    public Notification save(Notification notification) {
        NotificationEntity saved = repository.save(toEntity(notification));
        return toDomain(saved);
    }

    @Override
    public Optional<Notification> findByClinicIdAndId(UUID clinicId, UUID notificationId) {
        return repository.findByClinicIdAndId(clinicId, notificationId).map(this::toDomain);
    }

    @Override
    public Optional<Notification> findByClinicIdAndSourceKey(UUID clinicId, String sourceModule, String sourceKey) {
        return repository.findByClinicIdAndSourceModuleAndSourceKey(clinicId, sourceModule, sourceKey).map(this::toDomain);
    }

    @Override
    public List<Notification> findByClinicId(UUID clinicId, boolean unreadOnly, int limit) {
        return repository.findVisibleByClinic(clinicId, unreadOnly, LocalDateTime.now(), PageRequest.of(0, limit))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long countUnreadByClinicId(UUID clinicId) {
        return repository.countUnreadByClinic(clinicId, LocalDateTime.now());
    }

    @Override
    public void markAllRead(UUID clinicId) {
        repository.markAllRead(clinicId, LocalDateTime.now());
    }

    private NotificationEntity toEntity(Notification source) {
        return NotificationEntity.builder()
                .id(source.getId())
                .clinicId(source.getClinicId())
                .sourceModule(source.getSourceModule())
                .sourceKey(source.getSourceKey())
                .severity(source.getSeverity())
                .title(source.getTitle())
                .message(source.getMessage())
                .actionPath(source.getActionPath())
                .createdAt(source.getCreatedAt())
                .expiresAt(source.getExpiresAt())
                .readAt(source.getReadAt())
                .build();
    }

    private Notification toDomain(NotificationEntity source) {
        return Notification.builder()
                .id(source.getId())
                .clinicId(source.getClinicId())
                .sourceModule(source.getSourceModule())
                .sourceKey(source.getSourceKey())
                .severity(source.getSeverity())
                .title(source.getTitle())
                .message(source.getMessage())
                .actionPath(source.getActionPath())
                .createdAt(source.getCreatedAt())
                .expiresAt(source.getExpiresAt())
                .readAt(source.getReadAt())
                .build();
    }
}
