package com.jclinical.accounting.domain.ports.in;

import com.jclinical.accounting.domain.model.BankAccount;
import com.jclinical.accounting.domain.model.BankAccountType;
import com.jclinical.accounting.domain.model.CreditAccountAlert;
import com.jclinical.accounting.domain.model.JournalEntry;
import com.jclinical.accounting.domain.model.OpeningBalanceSetup;
import com.jclinical.accounting.domain.model.OperationalAccountKind;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ManageOpeningBalancesUseCase {

    OpeningBalanceSetup configureOpeningBalances(UUID actingUserId, UUID clinicId, ConfigureOpeningBalancesCommand command);

    Optional<OpeningBalanceSetup> getOpeningBalances(UUID actingUserId, UUID clinicId);

    /** Sin control de permiso: lo consumen validadores internos de caja, clinicas y nomina. */
    List<BankAccount> listBankAccounts(UUID clinicId);

    /** Ruta interna (scheduler de notificaciones de tarjetas de credito): sin control de permiso. */
    default List<CreditAccountAlert> listCreditAccountAlerts(UUID clinicId, LocalDate today, int withinDays) {
        return listCreditAccountAlerts(null, clinicId, today, withinDays);
    }

    List<CreditAccountAlert> listCreditAccountAlerts(UUID actingUserId, UUID clinicId, LocalDate today, int withinDays);

    /** Ruta interna (aprovisionamiento de caja chica al crear la clinica): sin control de permiso. */
    default BankAccount createBankAccount(UUID clinicId, CreateBankAccountCommand command) {
        return createBankAccount(null, clinicId, command);
    }

    BankAccount createBankAccount(UUID actingUserId, UUID clinicId, CreateBankAccountCommand command);

    BankAccount updateBankAccount(UUID actingUserId, UUID clinicId, UUID bankAccountId, UpdateBankAccountCommand command);

    BankAccount deactivateBankAccount(UUID actingUserId, UUID clinicId, UUID bankAccountId, DeactivateBankAccountCommand command);

    JournalEntry correctBankAccountBalance(UUID actingUserId, UUID clinicId, UUID bankAccountId, CorrectBankAccountBalanceCommand command);

    List<BankAccountMovement> listBankAccountMovements(UUID actingUserId, UUID clinicId, UUID bankAccountId);

    JournalEntry transferFunds(UUID actingUserId, UUID clinicId, TransferFundsCommand command);

    record ConfigureOpeningBalancesCommand(
            LocalDate entryDate,
            BigDecimal cashOpeningAmount,
            BigDecimal inventoryOpeningAmount,
            List<BankAccountOpeningCommand> bankAccounts,
            String notes
    ) {}

    record BankAccountOpeningCommand(
            String bankName,
            String alias,
            String accountLast4,
            String currency,
            BigDecimal openingBalance,
            BankAccountType accountType,
            OperationalAccountKind accountKind
    ) {}

    record CreateBankAccountCommand(
            String bankName,
            String alias,
            String accountLast4,
            String currency,
            BigDecimal openingBalance,
            BankAccountType accountType,
            OperationalAccountKind accountKind,
            LocalDate openingDate,
            LocalDate creditCutoffDate,
            LocalDate creditPaymentDueDate,
            BigDecimal creditLimit,
            BigDecimal creditCurrentAmount,
            BigDecimal creditMinimumPayment,
            BigDecimal creditNoInterestPayment,
            BigDecimal creditCurrentPaymentDue,
            String notes
    ) {}

    record UpdateBankAccountCommand(
            String bankName,
            String alias,
            String accountLast4,
            String currency,
            BankAccountType accountType,
            OperationalAccountKind accountKind,
            LocalDate creditCutoffDate,
            LocalDate creditPaymentDueDate,
            BigDecimal creditLimit,
            BigDecimal creditCurrentAmount,
            BigDecimal creditMinimumPayment,
            BigDecimal creditNoInterestPayment,
            BigDecimal creditCurrentPaymentDue,
            String reason
    ) {}

    record CorrectBankAccountBalanceCommand(
            LocalDate entryDate,
            BigDecimal correctedBalance,
            String reason
    ) {}

    record DeactivateBankAccountCommand(
            LocalDate entryDate,
            String reason
    ) {}

    record TransferFundsCommand(
            UUID sourceAccountId,
            UUID destinationAccountId,
            BigDecimal amount,
            LocalDate entryDate,
            String description
    ) {}

    record BankAccountMovement(
            UUID journalEntryId,
            UUID journalLineId,
            LocalDate entryDate,
            String description,
            String sourceEventType,
            BigDecimal debit,
            BigDecimal credit,
            BigDecimal movementAmount,
            BigDecimal balanceAfter
    ) {}
}
