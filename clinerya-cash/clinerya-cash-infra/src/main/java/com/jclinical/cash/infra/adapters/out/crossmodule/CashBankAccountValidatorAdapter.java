package com.jclinical.cash.infra.adapters.out.crossmodule;

import com.jclinical.accounting.domain.model.BankAccount;
import com.jclinical.accounting.domain.model.BankAccountType;
import com.jclinical.accounting.domain.ports.in.ManageOpeningBalancesUseCase;
import com.jclinical.cash.domain.ports.out.CashBankAccountValidatorPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CashBankAccountValidatorAdapter implements CashBankAccountValidatorPort {

    private final ManageOpeningBalancesUseCase openingBalancesUseCase;

    @Override
    public Optional<BankAccountSnapshot> findActiveDebitAccount(UUID bankAccountId, UUID clinicId) {
        if (bankAccountId == null || clinicId == null) {
            return Optional.empty();
        }
        return openingBalancesUseCase.listBankAccountsForSystem(clinicId).stream()
                .filter(account -> bankAccountId.equals(account.getId()))
                .filter(BankAccount::isActive)
                .filter(account -> account.getAccountType() == BankAccountType.DEBIT)
                .findFirst()
                .map(account -> new BankAccountSnapshot(account.getId(), account.displayName()));
    }
}
