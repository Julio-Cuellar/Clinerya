package com.jclinical.accounting.domain.ports.out;

import com.jclinical.accounting.domain.model.BankAccount;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BankAccountRepositoryPort {
    BankAccount save(BankAccount bankAccount);

    List<BankAccount> saveAll(List<BankAccount> bankAccounts);

    List<BankAccount> findByClinicId(UUID clinicId);

    Optional<BankAccount> findByIdAndClinicId(UUID bankAccountId, UUID clinicId);
}
