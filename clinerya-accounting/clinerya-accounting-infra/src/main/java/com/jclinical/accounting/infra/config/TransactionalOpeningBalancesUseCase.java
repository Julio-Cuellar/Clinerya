package com.jclinical.accounting.infra.config;

import com.jclinical.accounting.domain.model.BankAccount;
import com.jclinical.accounting.domain.model.CreditAccountAlert;
import com.jclinical.accounting.domain.model.JournalEntry;
import com.jclinical.accounting.domain.model.OpeningBalanceSetup;
import com.jclinical.accounting.domain.ports.in.ManageOpeningBalancesUseCase;
import com.jclinical.accounting.domain.service.OpeningBalanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Primary
@RequiredArgsConstructor
public class TransactionalOpeningBalancesUseCase implements ManageOpeningBalancesUseCase {

    private final OpeningBalanceService openingBalanceService;

    @Override
    @Transactional
    public OpeningBalanceSetup configureOpeningBalances(UUID actingUserId, UUID clinicId, ConfigureOpeningBalancesCommand command) {
        return openingBalanceService.configureOpeningBalances(actingUserId, clinicId, command);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OpeningBalanceSetup> getOpeningBalances(UUID actingUserId, UUID clinicId) {
        return openingBalanceService.getOpeningBalances(actingUserId, clinicId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BankAccount> listBankAccounts(UUID clinicId) {
        return openingBalanceService.listBankAccounts(clinicId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CreditAccountAlert> listCreditAccountAlerts(UUID actingUserId, UUID clinicId, java.time.LocalDate today, int withinDays) {
        return openingBalanceService.listCreditAccountAlerts(actingUserId, clinicId, today, withinDays);
    }

    @Override
    @Transactional
    public BankAccount createBankAccount(UUID actingUserId, UUID clinicId, CreateBankAccountCommand command) {
        return openingBalanceService.createBankAccount(actingUserId, clinicId, command);
    }

    @Override
    @Transactional
    public BankAccount updateBankAccount(UUID actingUserId, UUID clinicId, UUID bankAccountId, UpdateBankAccountCommand command) {
        return openingBalanceService.updateBankAccount(actingUserId, clinicId, bankAccountId, command);
    }

    @Override
    @Transactional
    public BankAccount deactivateBankAccount(UUID actingUserId, UUID clinicId, UUID bankAccountId, DeactivateBankAccountCommand command) {
        return openingBalanceService.deactivateBankAccount(actingUserId, clinicId, bankAccountId, command);
    }

    @Override
    @Transactional
    public JournalEntry correctBankAccountBalance(UUID actingUserId, UUID clinicId, UUID bankAccountId, CorrectBankAccountBalanceCommand command) {
        return openingBalanceService.correctBankAccountBalance(actingUserId, clinicId, bankAccountId, command);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BankAccountMovement> listBankAccountMovements(UUID actingUserId, UUID clinicId, UUID bankAccountId) {
        return openingBalanceService.listBankAccountMovements(actingUserId, clinicId, bankAccountId);
    }

    @Override
    @Transactional
    public JournalEntry transferFunds(UUID actingUserId, UUID clinicId, TransferFundsCommand command) {
        return openingBalanceService.transferFunds(actingUserId, clinicId, command);
    }
}
