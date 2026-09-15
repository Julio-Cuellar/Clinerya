package com.jclinical.notifications.domain.service;

import com.jclinical.core.security.ClinicAccessDeniedException;
import com.jclinical.core.security.StaffPermission;
import com.jclinical.core.security.StaffPermissionCheckerPort;
import com.jclinical.notifications.domain.model.Notification;
import com.jclinical.notifications.domain.ports.in.ManageNotificationsUseCase;
import com.jclinical.notifications.domain.ports.out.NotificationRepositoryPort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class NotificationService implements ManageNotificationsUseCase {

    private final NotificationRepositoryPort repository;
    private final StaffPermissionCheckerPort permissionChecker;

    public NotificationService(NotificationRepositoryPort repository, StaffPermissionCheckerPort permissionChecker) {
        this.repository = repository;
        this.permissionChecker = permissionChecker;
    }

    @Override
    public Notification publish(PublishNotificationCommand command) {
        validate(command);
        return repository.findByClinicIdAndSourceKey(command.clinicId(), command.sourceModule(), command.sourceKey())
                .map(existing -> refresh(existing, command))
                .orElseGet(() -> repository.save(Notification.builder()
                        .id(UUID.randomUUID())
                        .clinicId(command.clinicId())
                        .sourceModule(command.sourceModule().trim())
                        .sourceKey(command.sourceKey().trim())
                        .severity(normalizeSeverity(command.severity()))
                        .title(command.title().trim())
                        .message(command.message().trim())
                        .actionPath(clean(command.actionPath()))
                        .createdAt(LocalDateTime.now())
                        .expiresAt(command.expiresAt())
                        .build()));
    }

    @Override
    public NotificationList list(UUID clinicId, UUID actingUserId, boolean unreadOnly, int limit) {
        if (clinicId == null) throw new IllegalArgumentException("La clínica es obligatoria.");
        requireNotificationsAccess(clinicId, actingUserId);
        int safeLimit = Math.max(1, Math.min(limit, 100));
        return new NotificationList(
                repository.findByClinicId(clinicId, unreadOnly, safeLimit),
                repository.countUnreadByClinicId(clinicId)
        );
    }

    @Override
    public Notification markRead(UUID clinicId, UUID actingUserId, UUID notificationId) {
        requireNotificationsAccess(clinicId, actingUserId);
        Notification notification = repository.findByClinicIdAndId(clinicId, notificationId)
                .orElseThrow(() -> new IllegalArgumentException("La notificación no existe en esta clínica."));
        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now());
            return repository.save(notification);
        }
        return notification;
    }

    @Override
    public void markAllRead(UUID clinicId, UUID actingUserId) {
        requireNotificationsAccess(clinicId, actingUserId);
        repository.markAllRead(clinicId);
    }

    private void requireNotificationsAccess(UUID clinicId, UUID actingUserId) {
        if (actingUserId == null) {
            throw new ClinicAccessDeniedException("Usuario no autenticado.");
        }
        if (!permissionChecker.hasPermission(clinicId, actingUserId, StaffPermission.VIEW_NOTIFICATIONS)) {
            throw new ClinicAccessDeniedException("No tienes permiso para ver las notificaciones de esta clinica.");
        }
    }

    private Notification refresh(Notification existing, PublishNotificationCommand command) {
        boolean changed = !existing.getSeverity().equals(normalizeSeverity(command.severity()))
                || !existing.getTitle().equals(command.title().trim())
                || !existing.getMessage().equals(command.message().trim());
        existing.setSeverity(normalizeSeverity(command.severity()));
        existing.setTitle(command.title().trim());
        existing.setMessage(command.message().trim());
        existing.setActionPath(clean(command.actionPath()));
        existing.setExpiresAt(command.expiresAt());
        if (changed) {
            existing.setReadAt(null);
        }
        return repository.save(existing);
    }

    private void validate(PublishNotificationCommand command) {
        if (command == null || command.clinicId() == null) throw new IllegalArgumentException("La clínica es obligatoria.");
        if (blank(command.sourceModule()) || blank(command.sourceKey()) || blank(command.title()) || blank(command.message())) {
            throw new IllegalArgumentException("La notificación debe incluir origen, clave, título y mensaje.");
        }
    }

    private String normalizeSeverity(String severity) {
        String normalized = severity == null ? "WARNING" : severity.trim().toUpperCase();
        return switch (normalized) {
            case "CRITICAL", "WARNING", "INFO", "SUCCESS" -> normalized;
            default -> throw new IllegalArgumentException("La prioridad de la notificación no es válida.");
        };
    }

    private String clean(String value) {
        return blank(value) ? null : value.trim();
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
