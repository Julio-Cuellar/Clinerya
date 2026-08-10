package com.jclinical.cash.domain.ports.out;

import java.util.Optional;
import java.util.UUID;

public interface CashBankAccountValidatorPort {

    Optional<BankAccountSnapshot> findActiveDebitAccount(UUID bankAccountId, UUID clinicId);

    record BankAccountSnapshot(UUID bankAccountId, String displayName) {}
}
