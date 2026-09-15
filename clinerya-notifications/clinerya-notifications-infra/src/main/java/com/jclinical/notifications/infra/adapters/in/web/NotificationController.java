package com.jclinical.notifications.infra.adapters.in.web;

import com.jclinical.notifications.domain.model.Notification;
import com.jclinical.notifications.domain.ports.in.ManageNotificationsUseCase;
import com.jclinical.notifications.infra.adapters.in.web.dto.NotificationListResponse;
import com.jclinical.notifications.infra.adapters.in.web.dto.NotificationResponse;
import com.jclinical.users.infra.security.CurrentUserResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clinics/{clinicId}/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final ManageNotificationsUseCase notificationsUseCase;
    private final CurrentUserResolver currentUserResolver;

    @GetMapping
    public ResponseEntity<NotificationListResponse> list(
            @PathVariable UUID clinicId,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "50") int limit) {
        ManageNotificationsUseCase.NotificationList result = notificationsUseCase.list(
                clinicId, currentUserResolver.getCurrentUserId(), unreadOnly, limit);
        return ResponseEntity.ok(new NotificationListResponse(
                result.items().stream().map(this::toResponse).toList(),
                result.unreadCount()
        ));
    }

    @PostMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponse> markRead(
            @PathVariable UUID clinicId,
            @PathVariable UUID notificationId) {
        return ResponseEntity.ok(toResponse(
                notificationsUseCase.markRead(clinicId, currentUserResolver.getCurrentUserId(), notificationId)));
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllRead(@PathVariable UUID clinicId) {
        notificationsUseCase.markAllRead(clinicId, currentUserResolver.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getSourceModule(),
                notification.getSourceKey(),
                notification.getSeverity(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getActionPath(),
                notification.getCreatedAt(),
                notification.getExpiresAt(),
                notification.getReadAt()
        );
    }
}
