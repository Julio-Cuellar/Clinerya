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
    public OpeningBalanceSetup configureOpeningBalances(UUID clinicId, ConfigureOpeningBalancesCommand command) {
        return openingBalanceService.configureOpeningBalances(clinicId, command);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OpeningBalanceSetup> getOpeningBalances(UUID clinicId) {
        return openingBalanceService.getOpeningBalances(clinicId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BankAccount> listBankAccounts(UUID clinicId) {
        return openingBalanceService.listBankAccounts(clinicId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CreditAccountAlert> listCreditAccountAlerts(UUID clinicId, java.time.LocalDate today, int withinDays) {
        return openingBalanceService.listCreditAccountAlerts(clinicId, today, withinDays);
    }

    @Override
    @Transactional
    public BankAccount createBankAccount(UUID clinicId, CreateBankAccountCommand command) {
        return openingBalanceService.createBankAccount(clinicId, command);
    }

    @Override
    @Transactional
    public BankAccount updateBankAccount(UUID clinicId, UUID bankAccountId, UpdateBankAccountCommand command) {
        return openingBalanceService.updateBankAccount(clinicId, bankAccountId, command);
    }

    @Override
    @Transactional
    public BankAccount deactivateBankAccount(UUID clinicId, UUID bankAccountId, DeactivateBankAccountCommand command) {
        return openingBalanceService.deactivateBankAccount(clinicId, bankAccountId, command);
    }

    @Override
    @Transactional
    public JournalEntry correctBankAccountBalance(UUID clinicId, UUID bankAccountId, CorrectBankAccountBalanceCommand command) {
        return openingBalanceService.correctBankAccountBalance(clinicId, bankAccountId, command);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BankAccountMovement> listBankAccountMovements(UUID clinicId, UUID bankAccountId) {
        return openingBalanceService.listBankAccountMovements(clinicId, bankAccountId);
    }

    @Override
    @Transactional
    public JournalEntry transferFunds(UUID clinicId, TransferFundsCommand command) {
        return openingBalanceService.transferFunds(clinicId, command);
    }
}
