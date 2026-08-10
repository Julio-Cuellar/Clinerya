package com.jclinical.cash.infra.adapters.out.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataCashFolioCounterRepository extends JpaRepository<CashFolioCounterEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<CashFolioCounterEntity> findById(UUID clinicId);
}
