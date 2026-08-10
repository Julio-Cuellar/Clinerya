package com.jclinical.cash.infra.adapters.out.persistence;

import com.jclinical.cash.domain.model.CashExpenseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataCashExpenseRepository extends JpaRepository<CashExpenseEntity, UUID> {

    Optional<CashExpenseEntity> findByIdAndClinicId(UUID id, UUID clinicId);

    List<CashExpenseEntity> findByCashSessionIdAndClinicId(UUID cashSessionId, UUID clinicId);

    @Query("select coalesce(sum(e.amount), 0) from CashExpenseEntity e "
            + "where e.cashSessionId = :cashSessionId and e.status = :status")
    BigDecimal sumActiveAmountBySession(
            @Param("cashSessionId") UUID cashSessionId,
            @Param("status") CashExpenseStatus status);
}
