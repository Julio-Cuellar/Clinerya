package com.jclinical.records.infra.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface SpringDataRecordAccessLogRepository extends JpaRepository<RecordAccessLogEntity, UUID> {
    List<RecordAccessLogEntity> findByPatientIdOrderByCreatedAtDescIdDesc(UUID patientId, Pageable pageable);

    @Query(value = """
            SELECT *
              FROM records.record_access_logs
             WHERE patient_id = :patientId
               AND (
                    created_at < :cursorCreatedAt
                    OR (created_at = :cursorCreatedAt AND id < :cursorId)
               )
             ORDER BY created_at DESC, id DESC
             LIMIT :pageSize
            """, nativeQuery = true)
    List<RecordAccessLogEntity> findPageAfterCursor(
            @Param("patientId") UUID patientId,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") UUID cursorId,
            @Param("pageSize") int pageSize);
}
