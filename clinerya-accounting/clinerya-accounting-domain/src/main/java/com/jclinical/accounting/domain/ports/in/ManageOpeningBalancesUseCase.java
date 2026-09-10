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

    OpeningBalanceSetup configureOpeningBalances(UUID clinicId, UUID actingUserId, ConfigureOpeningBalancesCommand command);

    Optional<OpeningBalanceSetup> getOpeningBalances(UUID clinicId, UUID actingUserId);

    List<BankAccount> listBankAccounts(UUID clinicId, UUID actingUserId);

    /**
     * Lectura interna para el modulo de nomina, que ya autorizo al usuario con su propio
     * permiso (MANAGE_PAYROLL) antes de resolver la cuenta operativa de pago. No exponer
     * desde un controlador.
     */
    List<BankAccount> listBankAccountsForSystem(UUID clinicId);

    List<CreditAccountAlert> listCreditAccountAlerts(UUID clinicId, UUID actingUserId, LocalDate today, int withinDays);

    /**
     * Lectura interna para el job programado que notifica alertas de tarjetas: no hay
     * usuario en la peticion, asi que no se autoriza contra permisos de staff. No exponer
     * desde un controlador.
     */
    List<CreditAccountAlert> listCreditAccountAlertsForSystem(UUID clinicId, LocalDate today, int withinDays);

    BankAccount createBankAccount(UUID clinicId, UUID actingUserId, CreateBankAccountCommand command);

    /**
     * Aprovisionamiento automatico de la caja chica al crear una clinica o al arrancar la
     * aplicacion: no hay usuario en la peticion. No exponer desde un controlador.
     */
    BankAccount createBankAccountForSystem(UUID clinicId, CreateBankAccountCommand command);

    BankAccount updateBankAccount(UUID clinicId, UUID bankAccountId, UUID actingUserId, UpdateBankAccountCommand command);

    BankAccount deactivateBankAccount(UUID clinicId, UUID bankAccountId, UUID actingUserId, DeactivateBankAccountCommand command);

    JournalEntry correctBankAccountBalance(UUID clinicId, UUID bankAccountId, UUID actingUserId, CorrectBankAccountBalanceCommand command);

    List<BankAccountMovement> listBankAccountMovements(UUID clinicId, UUID bankAccountId, UUID actingUserId);

    JournalEntry transferFunds(UUID clinicId, UUID actingUserId, TransferFundsCommand command);

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
