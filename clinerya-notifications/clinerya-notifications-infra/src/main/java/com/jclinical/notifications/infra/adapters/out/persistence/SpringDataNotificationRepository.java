package com.jclinical.notifications.infra.adapters.out.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataNotificationRepository extends JpaRepository<NotificationEntity, UUID> {

    Optional<NotificationEntity> findByClinicIdAndId(UUID clinicId, UUID id);

    Optional<NotificationEntity> findByClinicIdAndSourceModuleAndSourceKey(UUID clinicId, String sourceModule, String sourceKey);

    @Query("select n from NotificationEntity n where n.clinicId = :clinicId "
            + "and (:unreadOnly = false or n.readAt is null) "
            + "and (n.expiresAt is null or n.expiresAt > :now) "
            + "order by n.createdAt desc")
    List<NotificationEntity> findVisibleByClinic(
            @Param("clinicId") UUID clinicId,
            @Param("unreadOnly") boolean unreadOnly,
            @Param("now") LocalDateTime now,
            Pageable pageable);

    @Query("select count(n) from NotificationEntity n where n.clinicId = :clinicId "
            + "and n.readAt is null and (n.expiresAt is null or n.expiresAt > :now)")
    long countUnreadByClinic(@Param("clinicId") UUID clinicId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("update NotificationEntity n set n.readAt = :now where n.clinicId = :clinicId and n.readAt is null")
    int markAllRead(@Param("clinicId") UUID clinicId, @Param("now") LocalDateTime now);
}
