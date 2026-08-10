package com.jclinical.accounting.infra.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataOpeningBalanceSetupRepository extends JpaRepository<OpeningBalanceSetupEntity, UUID> {
    Optional<OpeningBalanceSetupEntity> findByClinicId(UUID clinicId);

    boolean existsByClinicId(UUID clinicId);
}
