package com.jclinical.integrations.infra.adapters.out.persistence;

import com.jclinical.integrations.domain.model.ExternalEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataExternalCalendarEventRepository extends JpaRepository<ExternalCalendarEventEntity, UUID> {

    Optional<ExternalCalendarEventEntity> findByClinicIdAndGoogleEventId(UUID clinicId, String googleEventId);

    @Query("SELECT e FROM ExternalCalendarEventEntity e " +
           "WHERE e.clinicId = :clinicId " +
           "AND e.status = :status " +
           "AND e.startTime < :to " +
           "AND e.endTime > :from")
    List<ExternalCalendarEventEntity> findByClinicIdAndDateRangeAndStatus(
            @Param("clinicId") UUID clinicId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("status") ExternalEventStatus status
    );
}
