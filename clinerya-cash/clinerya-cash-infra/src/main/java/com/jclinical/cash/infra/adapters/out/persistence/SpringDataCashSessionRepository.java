package com.jclinical.cash.infra.adapters.out.persistence;

import com.jclinical.cash.domain.model.CashSessionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataCashSessionRepository extends JpaRepository<CashSessionEntity, UUID> {

    Optional<CashSessionEntity> findByClinicIdAndStatus(UUID clinicId, CashSessionStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from CashSessionEntity s where s.clinicId = :clinicId and s.status = :status")
    Optional<CashSessionEntity> findByClinicIdAndStatusForUpdate(
            @Param("clinicId") UUID clinicId, @Param("status") CashSessionStatus status);

    Optional<CashSessionEntity> findByIdAndClinicId(UUID id, UUID clinicId);

    List<CashSessionEntity> findByClinicIdOrderByOpenedAtDesc(UUID clinicId);
}
