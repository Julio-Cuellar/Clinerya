package com.jclinical.accounting.infra.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataBankAccountRepository extends JpaRepository<BankAccountEntity, UUID> {
    List<BankAccountEntity> findByClinicIdOrderByCreatedAtAsc(UUID clinicId);

    Optional<BankAccountEntity> findByIdAndClinicId(UUID id, UUID clinicId);
}
