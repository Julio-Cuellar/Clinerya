package com.jclinical.notifications.domain.service;

import com.jclinical.core.security.ClinicAccessDeniedException;
import com.jclinical.core.security.StaffPermission;
import com.jclinical.core.security.StaffPermissionCheckerPort;
import com.jclinical.notifications.domain.model.Notification;
import com.jclinical.notifications.domain.ports.in.ManageNotificationsUseCase.NotificationList;
import com.jclinical.notifications.domain.ports.in.ManageNotificationsUseCase.PublishNotificationCommand;
import com.jclinical.notifications.domain.ports.out.NotificationRepositoryPort;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NotificationServiceTest {

    private final InMemoryNotificationRepository repository = new InMemoryNotificationRepository();
    private final UUID actingUserId = UUID.randomUUID();
    private final StaffPermissionCheckerPort permissionChecker = (clinic, userId, permission) ->
            actingUserId.equals(userId) && permission == StaffPermission.VIEW_NOTIFICATIONS;
    private final NotificationService service = new NotificationService(repository, permissionChecker);

    @Test
    void publishesNewNotificationWithNormalizedFields() {
        UUID clinicId = UUID.randomUUID();

        Notification notification = service.publish(new PublishNotificationCommand(
                clinicId,
                " inventory ",
                " low-stock ",
                " info ",
                " Bajo stock ",
                " Revisar material ",
                " /inventario ",
                LocalDateTime.parse("2026-08-10T10:00:00")));

        assertNotNull(notification.getId());
        assertEquals(clinicId, notification.getClinicId());
        assertEquals("inventory", notification.getSourceModule());
        assertEquals("low-stock", notification.getSourceKey());
        assertEquals("INFO", notification.getSeverity());
        assertEquals("Bajo stock", notification.getTitle());
        assertEquals("Revisar material", notification.getMessage());
        assertEquals("/inventario", notification.getActionPath());
        assertNotNull(notification.getCreatedAt());
    }

    @Test
    void refreshesExistingNotificationAndMarksItUnreadWhenContentChanges() {
        UUID clinicId = UUID.randomUUID();
        Notification original = service.publish(command(clinicId, "WARNING", "Stock bajo"));
        original.setReadAt(LocalDateTime.parse("2026-08-09T11:00:00"));
        repository.save(original);

        Notification refreshed = service.publish(command(clinicId, "CRITICAL", "Stock agotado"));

        assertEquals(original.getId(), refreshed.getId());
        assertEquals("CRITICAL", refreshed.getSeverity());
        assertEquals("Stock agotado", refreshed.getTitle());
        assertNull(refreshed.getReadAt());
    }

    @Test
    void listClampsLimitAndCountsUnreadNotifications() {
        UUID clinicId = UUID.randomUUID();
        Notification first = service.publish(command(clinicId, "INFO", "Primera"));
        Notification second = service.publish(new PublishNotificationCommand(
                clinicId, "agenda", "late", "WARNING", "Retraso", "Paciente tarde", null, null));
        second.setReadAt(LocalDateTime.now());
        repository.save(second);

        NotificationList list = service.list(clinicId, actingUserId, false, 500);

        assertEquals(2, list.items().size());
        assertEquals(1, list.unreadCount());
        List<UUID> ids = list.items().stream().map(Notification::getId).toList();
        assertEquals(2, ids.size());
        assertTrue(ids.contains(first.getId()));
        assertTrue(ids.contains(second.getId()));
    }

    @Test
    void markReadOnlyUpdatesUnreadNotification() {
        UUID clinicId = UUID.randomUUID();
        Notification notification = service.publish(command(clinicId, "WARNING", "Stock bajo"));

        Notification read = service.markRead(clinicId, actingUserId, notification.getId());
        Notification readAgain = service.markRead(clinicId, actingUserId, notification.getId());

        assertNotNull(read.getReadAt());
        assertEquals(read.getReadAt(), readAgain.getReadAt());
    }

    @Test
    void rejectsInvalidCommands() {
        UUID clinicId = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class, () -> service.publish(null));
        PublishNotificationCommand invalidSeverityCommand =
                new PublishNotificationCommand(clinicId, "inventory", "key", "LOUD", "Titulo", "Mensaje", null, null);
        assertThrows(IllegalArgumentException.class, () -> service.publish(invalidSeverityCommand));
        assertThrows(IllegalArgumentException.class, () -> service.list(null, actingUserId, false, 10));
        UUID missingNotificationId = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () -> service.markRead(clinicId, actingUserId, missingNotificationId));
    }

    @Test
    void markAllReadMarksEveryClinicNotification() {
        UUID clinicId = UUID.randomUUID();
        UUID otherClinicId = UUID.randomUUID();
        service.publish(command(clinicId, "WARNING", "Primera"));
        service.publish(new PublishNotificationCommand(clinicId, "agenda", "late", "WARNING", "Retraso", "Paciente tarde", null, null));
        service.publish(command(otherClinicId, "WARNING", "Otra"));

        service.markAllRead(clinicId, actingUserId);

        assertEquals(0, repository.countUnreadByClinicId(clinicId));
        assertEquals(1, repository.countUnreadByClinicId(otherClinicId));
    }

    @Test
    void readOperationsRequireViewNotificationsPermission() {
        UUID clinicId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        Notification notification = service.publish(command(clinicId, "WARNING", "Stock bajo"));

        assertThrows(ClinicAccessDeniedException.class, () -> service.list(clinicId, strangerId, false, 10));
        assertThrows(ClinicAccessDeniedException.class, () -> service.list(clinicId, null, false, 10));
        assertThrows(ClinicAccessDeniedException.class,
                () -> service.markRead(clinicId, strangerId, notification.getId()));
        assertThrows(ClinicAccessDeniedException.class, () -> service.markAllRead(clinicId, strangerId));
    }

    private static PublishNotificationCommand command(UUID clinicId, String severity, String title) {
        return new PublishNotificationCommand(
                clinicId,
                "inventory",
                "low-stock",
                severity,
                title,
                "Revisar material",
                "/inventario",
                null);
    }

    private static final class InMemoryNotificationRepository implements NotificationRepositoryPort {
        private final List<Notification> notifications = new ArrayList<>();

        @Override
        public Notification save(Notification notification) {
            notifications.removeIf(existing -> existing.getId().equals(notification.getId()));
            notifications.add(notification);
            return notification;
        }

        @Override
        public Optional<Notification> findByClinicIdAndId(UUID clinicId, UUID notificationId) {
            return notifications.stream()
                    .filter(notification -> clinicId.equals(notification.getClinicId()))
                    .filter(notification -> notificationId.equals(notification.getId()))
                    .findFirst();
        }

        @Override
        public Optional<Notification> findByClinicIdAndSourceKey(UUID clinicId, String sourceModule, String sourceKey) {
            return notifications.stream()
                    .filter(notification -> clinicId.equals(notification.getClinicId()))
                    .filter(notification -> sourceModule.trim().equals(notification.getSourceModule()))
                    .filter(notification -> sourceKey.trim().equals(notification.getSourceKey()))
                    .findFirst();
        }

        @Override
        public List<Notification> findByClinicId(UUID clinicId, boolean unreadOnly, int limit) {
            return notifications.stream()
                    .filter(notification -> clinicId.equals(notification.getClinicId()))
                    .filter(notification -> !unreadOnly || notification.getReadAt() == null)
                    .sorted(Comparator.comparing(Notification::getCreatedAt))
                    .limit(limit)
                    .toList();
        }

        @Override
        public long countUnreadByClinicId(UUID clinicId) {
            return notifications.stream()
                    .filter(notification -> clinicId.equals(notification.getClinicId()))
                    .filter(notification -> notification.getReadAt() == null)
                    .count();
        }

        @Override
        public void markAllRead(UUID clinicId) {
            LocalDateTime now = LocalDateTime.now();
            notifications.stream()
                    .filter(notification -> clinicId.equals(notification.getClinicId()))
                    .filter(notification -> notification.getReadAt() == null)
                    .forEach(notification -> notification.setReadAt(now));
        }
    }
}
