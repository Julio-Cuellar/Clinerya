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
    public OpeningBalanceSetup configureOpeningBalances(UUID clinicId, UUID actingUserId, ConfigureOpeningBalancesCommand command) {
        return openingBalanceService.configureOpeningBalances(clinicId, actingUserId, command);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OpeningBalanceSetup> getOpeningBalances(UUID clinicId, UUID actingUserId) {
        return openingBalanceService.getOpeningBalances(clinicId, actingUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BankAccount> listBankAccounts(UUID clinicId, UUID actingUserId) {
        return openingBalanceService.listBankAccounts(clinicId, actingUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BankAccount> listBankAccountsForSystem(UUID clinicId) {
        return openingBalanceService.listBankAccountsForSystem(clinicId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CreditAccountAlert> listCreditAccountAlerts(UUID clinicId, UUID actingUserId, java.time.LocalDate today, int withinDays) {
        return openingBalanceService.listCreditAccountAlerts(clinicId, actingUserId, today, withinDays);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CreditAccountAlert> listCreditAccountAlertsForSystem(UUID clinicId, java.time.LocalDate today, int withinDays) {
        return openingBalanceService.listCreditAccountAlertsForSystem(clinicId, today, withinDays);
    }

    @Override
    @Transactional
    public BankAccount createBankAccount(UUID clinicId, UUID actingUserId, CreateBankAccountCommand command) {
        return openingBalanceService.createBankAccount(clinicId, actingUserId, command);
    }

    @Override
    @Transactional
    public BankAccount createBankAccountForSystem(UUID clinicId, CreateBankAccountCommand command) {
        return openingBalanceService.createBankAccountForSystem(clinicId, command);
    }

    @Override
    @Transactional
    public BankAccount updateBankAccount(UUID clinicId, UUID bankAccountId, UUID actingUserId, UpdateBankAccountCommand command) {
        return openingBalanceService.updateBankAccount(clinicId, bankAccountId, actingUserId, command);
    }

    @Override
    @Transactional
    public BankAccount deactivateBankAccount(UUID clinicId, UUID bankAccountId, UUID actingUserId, DeactivateBankAccountCommand command) {
        return openingBalanceService.deactivateBankAccount(clinicId, bankAccountId, actingUserId, command);
    }

    @Override
    @Transactional
    public JournalEntry correctBankAccountBalance(UUID clinicId, UUID bankAccountId, UUID actingUserId, CorrectBankAccountBalanceCommand command) {
        return openingBalanceService.correctBankAccountBalance(clinicId, bankAccountId, actingUserId, command);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BankAccountMovement> listBankAccountMovements(UUID clinicId, UUID bankAccountId, UUID actingUserId) {
        return openingBalanceService.listBankAccountMovements(clinicId, bankAccountId, actingUserId);
    }

    @Override
    @Transactional
    public JournalEntry transferFunds(UUID clinicId, UUID actingUserId, TransferFundsCommand command) {
        return openingBalanceService.transferFunds(clinicId, actingUserId, command);
    }
}
