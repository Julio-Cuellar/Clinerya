package com.jclinical.accounting.domain.service;

import com.jclinical.accounting.domain.model.BankAccount;
import com.jclinical.accounting.domain.model.BankAccountType;
import com.jclinical.accounting.domain.model.CreditAccountAlert;
import com.jclinical.accounting.domain.model.JournalEntry;
import com.jclinical.accounting.domain.model.JournalLine;
import com.jclinical.accounting.domain.model.OpeningBalanceSetup;
import com.jclinical.accounting.domain.model.OperationalAccountKind;
import com.jclinical.accounting.domain.ports.in.ManageOpeningBalancesUseCase;
import com.jclinical.accounting.domain.ports.out.BankAccountRepositoryPort;
import com.jclinical.accounting.domain.ports.out.JournalEntryRepositoryPort;
import com.jclinical.accounting.domain.ports.out.OpeningBalanceSetupRepositoryPort;
import com.jclinical.core.security.ClinicAccessDeniedException;
import com.jclinical.core.security.StaffPermission;
import com.jclinical.core.security.StaffPermissionCheckerPort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.temporal.ChronoUnit;

public class OpeningBalanceService implements ManageOpeningBalancesUseCase {

    private static final String CAJA_OPERATIVA = "11100";
    private static final String CAJA_OPERATIVA_NOMBRE = "Caja Operativa";
    private static final String CAJA_CHICA = "11150";
    private static final String CAJA_CHICA_NOMBRE = "Caja Chica";
    private static final String BANCOS = "11200";
    private static final String BANCOS_NOMBRE = "Bancos";
    private static final String FONDOS_ESPECIFICOS = "11300";
    private static final String FONDOS_ESPECIFICOS_NOMBRE = "Fondos especificos";
    private static final String OTRAS_DISPONIBILIDADES = "11400";
    private static final String OTRAS_DISPONIBILIDADES_NOMBRE = "Otras disponibilidades";
    private static final String CREDITO_BANCARIO = "21200";
    private static final String CREDITO_BANCARIO_NOMBRE = "Creditos bancarios";
    private static final String ALMACEN_INSUMOS = "12100";
    private static final String ALMACEN_INSUMOS_NOMBRE = "Almacen de Insumos Clinicos";
    private static final String CAPITAL_INICIAL = "31000";
    private static final String CAPITAL_INICIAL_NOMBRE = "Capital inicial";

    private final OpeningBalanceSetupRepositoryPort setupRepository;
    private final BankAccountRepositoryPort bankAccountRepository;
    private final JournalEntryRepositoryPort journalEntryRepository;
    private final StaffPermissionCheckerPort permissionChecker;

    public OpeningBalanceService(
            OpeningBalanceSetupRepositoryPort setupRepository,
            BankAccountRepositoryPort bankAccountRepository,
            JournalEntryRepositoryPort journalEntryRepository,
            StaffPermissionCheckerPort permissionChecker) {
        this.setupRepository = setupRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.journalEntryRepository = journalEntryRepository;
        this.permissionChecker = permissionChecker;
    }

    private void requirePermission(UUID clinicId, UUID actingUserId, StaffPermission permission, String deniedMessage) {
        if (actingUserId == null) {
            throw new ClinicAccessDeniedException("Usuario no autenticado.");
        }
        if (!permissionChecker.hasPermission(clinicId, actingUserId, permission)) {
            throw new ClinicAccessDeniedException(deniedMessage);
        }
    }

    private void requireManageBankAccounts(UUID clinicId, UUID actingUserId) {
        requirePermission(clinicId, actingUserId, StaffPermission.MANAGE_BANK_ACCOUNTS,
                "No tienes permiso para gestionar las cuentas de esta clinica.");
    }

    private void requireViewAccounting(UUID clinicId, UUID actingUserId) {
        requirePermission(clinicId, actingUserId, StaffPermission.VIEW_ACCOUNTING,
                "No tienes permiso para consultar la contabilidad de esta clinica.");
    }

    @Override
    public OpeningBalanceSetup configureOpeningBalances(UUID actingUserId, UUID clinicId, ConfigureOpeningBalancesCommand command) {
        requirePermission(clinicId, actingUserId, StaffPermission.MANAGE_ACCOUNTING,
                "No tienes permiso para configurar los saldos iniciales de esta clinica.");
        if (clinicId == null) {
            throw new IllegalArgumentException("La clinica es obligatoria.");
        }
        if (command == null) {
            throw new IllegalArgumentException("Los saldos iniciales son obligatorios.");
        }
        if (setupRepository.existsByClinicId(clinicId)) {
            throw new IllegalStateException("Los saldos iniciales ya fueron registrados para esta clinica.");
        }

        LocalDate entryDate = command.entryDate() != null ? command.entryDate() : LocalDate.now();
        BigDecimal cashOpeningAmount = nonNegative(command.cashOpeningAmount(), "El saldo inicial de caja no puede ser negativo.");
        BigDecimal inventoryOpeningAmount = nonNegative(command.inventoryOpeningAmount(), "El saldo inicial de inventario no puede ser negativo.");
        List<BankAccount> bankAccounts = buildBankAccounts(clinicId, entryDate, command.bankAccounts());

        BigDecimal bankTotal = bankAccounts.stream()
                .map(BankAccount::getOpeningBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalOpeningAssets = cashOpeningAmount.add(inventoryOpeningAmount).add(bankTotal);
        if (totalOpeningAssets.signum() <= 0) {
            throw new IllegalArgumentException("Registra al menos un saldo inicial mayor a cero.");
        }

        LocalDateTime now = now();
        UUID setupId = UUID.randomUUID();
        UUID journalEntryId = UUID.randomUUID();
        UUID sourceEventId = UUID.randomUUID();

        List<JournalLine> lines = new ArrayList<>();
        addDebitLine(lines, CAJA_OPERATIVA, CAJA_OPERATIVA_NOMBRE, cashOpeningAmount);
        bankAccounts.stream()
                .filter(account -> account.getOpeningBalance().signum() > 0)
                .forEach(account -> addBankOpeningLine(lines, account));
        addDebitLine(lines, ALMACEN_INSUMOS, ALMACEN_INSUMOS_NOMBRE, inventoryOpeningAmount);
        addCapitalBalancingLine(lines, "Capital inicial por apertura");

        JournalEntry entry = JournalEntry.builder()
                .id(journalEntryId)
                .clinicId(clinicId)
                .description(buildDescription(command.notes()))
                .entryDate(entryDate)
                .sourceEventType("SaldoInicial")
                .sourceEventId(sourceEventId)
                .createdAt(now)
                .lines(lines)
                .build();
        entry.validateBalanced();

        JournalEntry savedEntry = journalEntryRepository.save(entry);

        OpeningBalanceSetup setup = OpeningBalanceSetup.builder()
                .id(setupId)
                .clinicId(clinicId)
                .entryDate(entryDate)
                .cashOpeningAmount(cashOpeningAmount)
                .inventoryOpeningAmount(inventoryOpeningAmount)
                .totalOpeningAssets(totalOpeningAssets)
                .capitalAccountCode(CAPITAL_INICIAL)
                .capitalAccountName(CAPITAL_INICIAL_NOMBRE)
                .journalEntryId(savedEntry.getId())
                .notes(clean(command.notes()))
                .createdAt(now)
                .build();

        OpeningBalanceSetup savedSetup = setupRepository.save(setup);

        bankAccounts.forEach(account -> {
            account.setOpeningBalanceSetupId(savedSetup.getId());
            account.setOpeningJournalEntryId(savedEntry.getId());
            account.setCreatedAt(now);
            account.setUpdatedAt(now);
        });
        List<BankAccount> savedBankAccounts = bankAccountRepository.saveAll(bankAccounts);
        savedSetup.setBankAccounts(savedBankAccounts);
        return savedSetup;
    }

    @Override
    public Optional<OpeningBalanceSetup> getOpeningBalances(UUID actingUserId, UUID clinicId) {
        requireViewAccounting(clinicId, actingUserId);
        return setupRepository.findByClinicId(clinicId)
                .map(setup -> {
                    setup.setBankAccounts(bankAccountRepository.findByClinicId(clinicId));
                    return setup;
                });
    }

    @Override
    public List<BankAccount> listBankAccounts(UUID clinicId) {
        return bankAccountRepository.findByClinicId(clinicId);
    }

    @Override
    public List<CreditAccountAlert> listCreditAccountAlerts(UUID actingUserId, UUID clinicId, LocalDate today, int withinDays) {
        if (actingUserId != null) {
            requireViewAccounting(clinicId, actingUserId);
        }
        LocalDate currentDate = today != null ? today : LocalDate.now();
        int alertWindow = Math.max(0, Math.min(withinDays, 30));
        LocalDate lastAlertDate = currentDate.plusDays(alertWindow);

        return bankAccountRepository.findByClinicId(clinicId).stream()
                .filter(account -> account.isActive() && account.getAccountType() == BankAccountType.CREDIT)
                .flatMap(account -> java.util.stream.Stream.of(
                        toCreditAlert(account, CreditAccountAlert.AlertType.CUTOFF, account.getCreditCutoffDate(), currentDate, lastAlertDate),
                        toCreditAlert(account, CreditAccountAlert.AlertType.PAYMENT_DUE, account.getCreditPaymentDueDate(), currentDate, lastAlertDate)
                ))
                .filter(alert -> alert != null)
                .sorted((left, right) -> {
                    int dateOrder = left.alertDate().compareTo(right.alertDate());
                    return dateOrder != 0 ? dateOrder : left.type().compareTo(right.type());
                })
                .toList();
    }

    private CreditAccountAlert toCreditAlert(
            BankAccount account,
            CreditAccountAlert.AlertType type,
            LocalDate alertDate,
            LocalDate currentDate,
            LocalDate lastAlertDate) {
        if (alertDate == null || alertDate.isAfter(lastAlertDate)) {
            return null;
        }
        long daysRemaining = ChronoUnit.DAYS.between(currentDate, alertDate);
        boolean overdue = daysRemaining <= 0;
        String eventName = type == CreditAccountAlert.AlertType.CUTOFF ? "la fecha de corte" : "la fecha límite de pago";
        String title = type == CreditAccountAlert.AlertType.CUTOFF ? "Corte de tarjeta próximo" : "Pago de tarjeta próximo";
        String detail = daysRemaining < 0
                ? String.format("%s venció hace %d día(s) en %s.", capitalize(eventName), Math.abs(daysRemaining), account.displayName())
                : daysRemaining == 0
                ? String.format("Hoy es %s de %s.", eventName, account.displayName())
                : String.format("Faltan %d día(s) para %s de %s.", daysRemaining, eventName, account.displayName());
        return new CreditAccountAlert(
                account.getId(),
                account.displayName(),
                type,
                alertDate,
                daysRemaining,
                overdue ? "critical" : "warning",
                title,
                detail
        );
    }

    private String capitalize(String value) {
        return value.substring(0, 1).toUpperCase() + value.substring(1);
    }

    @Override
    public BankAccount createBankAccount(UUID actingUserId, UUID clinicId, CreateBankAccountCommand command) {
        if (actingUserId != null) {
            requireManageBankAccounts(clinicId, actingUserId);
        }
        validateClinicAndCommand(clinicId, command);

        LocalDate entryDate = command.openingDate() != null ? command.openingDate() : LocalDate.now();
        LocalDateTime now = now();
        BankAccount account = buildBankAccount(
                clinicId,
                entryDate,
                command.bankName(),
                command.alias(),
                command.accountLast4(),
                command.currency(),
                command.openingBalance(),
                command.accountType(),
                command.accountKind()
        );
        applyCreditMetadata(
                account,
                command.creditCutoffDate(),
                command.creditPaymentDueDate(),
                command.creditLimit(),
                command.creditCurrentAmount(),
                command.creditMinimumPayment(),
                command.creditNoInterestPayment(),
                command.creditCurrentPaymentDue()
        );
        account.setCreatedAt(now);
        account.setUpdatedAt(now);
        account = bankAccountRepository.save(account);

        if (account.getOpeningBalance().signum() > 0) {
            JournalEntry entry = createBankAccountAdjustmentEntry(
                    clinicId,
                    account,
                    account.getOpeningBalance(),
                    entryDate,
                    "Alta de cuenta operativa - " + account.displayName(),
                    account.getAccountKind() == OperationalAccountKind.BANK ? "CuentaBancariaAlta" : "CuentaOperativaAlta",
                    command.notes()
            );
            account.setOpeningJournalEntryId(entry.getId());
            account.setUpdatedAt(now());
            account = bankAccountRepository.save(account);
        }
        return account;
    }

    @Override
    public BankAccount updateBankAccount(UUID actingUserId, UUID clinicId, UUID bankAccountId, UpdateBankAccountCommand command) {
        requireManageBankAccounts(clinicId, actingUserId);
        if (command == null) {
            throw new IllegalArgumentException("Los datos de la cuenta bancaria son obligatorios.");
        }
        BankAccount account = getBankAccount(clinicId, bankAccountId);
        if (!account.isActive()) {
            throw new IllegalStateException("No se puede modificar una cuenta bancaria dada de baja.");
        }

        String reason = clean(command.reason());
        if (reason == null) {
            throw new IllegalArgumentException("Indica el motivo de la modificacion.");
        }

        BankAccountType currentType = account.getAccountType() != null ? account.getAccountType() : BankAccountType.DEBIT;
        BankAccountType newType = command.accountType() != null ? command.accountType() : currentType;
        OperationalAccountKind currentKind = account.getAccountKind() != null ? account.getAccountKind() : OperationalAccountKind.BANK;
        OperationalAccountKind newKind = command.accountKind() != null ? command.accountKind() : currentKind;
        if (newType != currentType && hasMovements(clinicId, account)) {
            throw new IllegalStateException("No se puede cambiar el tipo de una cuenta bancaria que ya tiene movimientos. Da de baja la cuenta y registra una nueva.");
        }
        if (newKind != currentKind && hasMovements(clinicId, account)) {
            throw new IllegalStateException("No se puede cambiar la clase de una cuenta que ya tiene movimientos. Da de baja la cuenta y registra una nueva.");
        }

        applyBankAccountMetadata(
                account,
                command.bankName(),
                command.alias(),
                command.accountLast4(),
                command.currency(),
                newType,
                newKind,
                command.creditCutoffDate(),
                command.creditPaymentDueDate(),
                command.creditLimit(),
                command.creditCurrentAmount(),
                command.creditMinimumPayment(),
                command.creditNoInterestPayment(),
                command.creditCurrentPaymentDue()
        );
        LocalDateTime now = now();
        account.setLastModifiedAt(now);
        account.setLastModificationReason(reason);
        account.setUpdatedAt(now);
        return bankAccountRepository.save(account);
    }

    @Override
    public BankAccount deactivateBankAccount(UUID actingUserId, UUID clinicId, UUID bankAccountId, DeactivateBankAccountCommand command) {
        requireManageBankAccounts(clinicId, actingUserId);
        if (bankAccountId == null) {
            throw new IllegalArgumentException("La cuenta bancaria es obligatoria.");
        }
        BankAccount account = getBankAccount(clinicId, bankAccountId);
        if (!account.isActive()) {
            return account;
        }
        LocalDate entryDate = command != null && command.entryDate() != null ? command.entryDate() : LocalDate.now();
        String reason = command != null ? clean(command.reason()) : null;
        BigDecimal currentBalance = currentBalance(clinicId, account);
        if (currentBalance.signum() > 0) {
            createBankAccountAdjustmentEntry(
                    clinicId,
                    account,
                    currentBalance.negate(),
                    entryDate,
                    "Baja de cuenta bancaria - " + account.displayName(),
                    "CuentaBancariaBaja",
                    reason
            );
        }
        account.setActive(false);
        account.setDeactivatedAt(now());
        account.setDeactivationReason(reason);
        account.setUpdatedAt(now());
        return bankAccountRepository.save(account);
    }

    @Override
    public JournalEntry correctBankAccountBalance(UUID actingUserId, UUID clinicId, UUID bankAccountId, CorrectBankAccountBalanceCommand command) {
        requireManageBankAccounts(clinicId, actingUserId);
        if (command == null) {
            throw new IllegalArgumentException("La correccion es obligatoria.");
        }
        BigDecimal correctedBalance = nonNegative(command.correctedBalance(), "El saldo corregido no puede ser negativo.");
        BankAccount account = getBankAccount(clinicId, bankAccountId);
        BigDecimal currentBalance = currentBalance(clinicId, account);
        BigDecimal delta = correctedBalance.subtract(currentBalance);
        if (delta.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("El saldo corregido es igual al saldo actual.");
        }
        LocalDate entryDate = command.entryDate() != null ? command.entryDate() : LocalDate.now();
        String reason = clean(command.reason());
        if (reason == null) {
            throw new IllegalArgumentException("Indica el motivo de la correccion.");
        }
        return createBankAccountAdjustmentEntry(
                clinicId,
                account,
                delta,
                entryDate,
                "Correccion de saldo bancario - " + account.displayName(),
                "CuentaBancariaCorreccion",
                reason
        );
    }

    @Override
    public List<BankAccountMovement> listBankAccountMovements(UUID actingUserId, UUID clinicId, UUID bankAccountId) {
        requireViewAccounting(clinicId, actingUserId);
        BankAccount account = getBankAccount(clinicId, bankAccountId);
        BigDecimal[] runningBalance = {BigDecimal.ZERO};
        return journalEntryRepository.findByClinicIdAndBankAccountId(clinicId, bankAccountId).stream()
                .flatMap(entry -> entry.getLines().stream()
                        .filter(line -> bankAccountId.equals(line.getBankAccountId()))
                        .map(line -> {
                            BigDecimal movementAmount = movementAmount(account, line);
                            runningBalance[0] = runningBalance[0].add(movementAmount);
                            return new BankAccountMovement(
                                    entry.getId(),
                                    line.getId(),
                                    entry.getEntryDate(),
                                    entry.getDescription(),
                                    entry.getSourceEventType(),
                                    line.getDebit(),
                                    line.getCredit(),
                                    movementAmount,
                                    runningBalance[0]
                            );
                        }))
                .toList();
    }

    @Override
    public JournalEntry transferFunds(UUID actingUserId, UUID clinicId, TransferFundsCommand command) {
        requireManageBankAccounts(clinicId, actingUserId);
        if (clinicId == null || command == null) {
            throw new IllegalArgumentException("La clinica y los datos de la transferencia son obligatorios.");
        }
        if (command.sourceAccountId() == null || command.destinationAccountId() == null) {
            throw new IllegalArgumentException("Selecciona la cuenta de origen y la cuenta de destino.");
        }
        if (command.sourceAccountId().equals(command.destinationAccountId())) {
            throw new IllegalArgumentException("La cuenta de origen y destino deben ser diferentes.");
        }
        BigDecimal amount = command.amount() != null ? command.amount() : BigDecimal.ZERO;
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("El monto de la transferencia debe ser mayor a cero.");
        }

        BankAccount source = getBankAccount(clinicId, command.sourceAccountId());
        BankAccount destination = getBankAccount(clinicId, command.destinationAccountId());
        ensureTransferable(source);
        ensureTransferable(destination);
        if (!source.getCurrency().equalsIgnoreCase(destination.getCurrency())) {
            throw new IllegalArgumentException("Las cuentas deben manejar la misma moneda para transferir fondos.");
        }
        BigDecimal sourceBalance = currentBalance(clinicId, source);
        if (sourceBalance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("La cuenta de origen no tiene saldo suficiente.");
        }

        LocalDate entryDate = command.entryDate() != null ? command.entryDate() : LocalDate.now();
        String description = clean(command.description()) == null
                ? "Transferencia entre cuentas operativas"
                : clean(command.description());
        JournalEntry entry = JournalEntry.builder()
                .id(UUID.randomUUID())
                .clinicId(clinicId)
                .description(description + " - " + source.displayName() + " a " + destination.displayName())
                .entryDate(entryDate)
                .sourceEventType("TransferenciaCuentaOperativa")
                .sourceEventId(UUID.randomUUID())
                .createdAt(now())
                .lines(List.of(
                        JournalLine.builder()
                                .id(UUID.randomUUID())
                                .bankAccountId(destination.getId())
                                .accountCode(accountCode(destination.getAccountKind(), destination.getAccountType()))
                                .accountName(accountName(destination.getAccountKind(), destination.getAccountType()) + " - " + destination.displayName())
                                .debit(amount)
                                .credit(BigDecimal.ZERO)
                                .build(),
                        JournalLine.builder()
                                .id(UUID.randomUUID())
                                .bankAccountId(source.getId())
                                .accountCode(accountCode(source.getAccountKind(), source.getAccountType()))
                                .accountName(accountName(source.getAccountKind(), source.getAccountType()) + " - " + source.displayName())
                                .debit(BigDecimal.ZERO)
                                .credit(amount)
                                .build()
                ))
                .build();
        entry.validateBalanced();
        return journalEntryRepository.save(entry);
    }

    private List<BankAccount> buildBankAccounts(
            UUID clinicId,
            LocalDate entryDate,
            List<BankAccountOpeningCommand> commands) {
        if (commands == null || commands.isEmpty()) {
            return List.of();
        }

        List<BankAccount> accounts = new ArrayList<>();
        for (BankAccountOpeningCommand command : commands) {
            if (command == null || isBlank(command.bankName()) && isBlank(command.alias()) && isBlank(command.openingBalance())) {
                continue;
            }
            accounts.add(buildBankAccount(
                    clinicId,
                    entryDate,
                    command.bankName(),
                    command.alias(),
                    command.accountLast4(),
                    command.currency(),
                    command.openingBalance(),
                    command.accountType(),
                    command.accountKind()
            ));
        }
        return accounts;
    }

    private BankAccount buildBankAccount(
            UUID clinicId,
            LocalDate entryDate,
            String bankName,
            String aliasValue,
            String accountLast4,
            String currencyValue,
            BigDecimal openingBalanceValue,
            BankAccountType accountType,
            OperationalAccountKind accountKind) {
        OperationalAccountKind kind = accountKind != null ? accountKind : OperationalAccountKind.BANK;
        BigDecimal openingBalance = nonNegative(openingBalanceValue, "El saldo inicial de la cuenta no puede ser negativo.");
        if (kind == OperationalAccountKind.BANK && isBlank(bankName)) {
            throw new IllegalArgumentException("El nombre del banco es obligatorio.");
        }
        if (kind != OperationalAccountKind.BANK && isBlank(bankName) && isBlank(aliasValue)) {
            throw new IllegalArgumentException("El nombre o referencia de la cuenta es obligatorio.");
        }
        String sourceName = isBlank(bankName) ? aliasValue.trim() : bankName.trim();
        String alias = isBlank(aliasValue) ? sourceName : aliasValue.trim();
        String currency = isBlank(currencyValue) ? "MXN" : currencyValue.trim().toUpperCase();
        if (currency.length() != 3) {
            throw new IllegalArgumentException("La moneda debe tener 3 caracteres, por ejemplo MXN.");
        }
        String last4 = clean(accountLast4);
        if (kind == OperationalAccountKind.BANK && last4 != null && !last4.matches("\\d{4}")) {
            throw new IllegalArgumentException("Los ultimos digitos de la cuenta bancaria deben ser 4 numeros.");
        }
        if (kind != OperationalAccountKind.BANK) {
            last4 = null;
        }
        BankAccountType type = accountType != null ? accountType : BankAccountType.DEBIT;

        return BankAccount.builder()
                .id(UUID.randomUUID())
                .clinicId(clinicId)
                .accountCode(accountCode(kind, type))
                .accountType(type)
                .accountKind(kind)
                .bankName(sourceName)
                .alias(alias)
                .accountLast4(last4)
                .currency(currency)
                .openingBalance(openingBalance)
                .openingDate(entryDate)
                .active(true)
                .build();
    }

    private void applyBankAccountMetadata(
            BankAccount account,
            String bankName,
            String aliasValue,
            String accountLast4,
            String currencyValue,
            BankAccountType accountType,
            OperationalAccountKind accountKind,
            LocalDate creditCutoffDate,
            LocalDate creditPaymentDueDate,
            BigDecimal creditLimit,
            BigDecimal creditCurrentAmount,
            BigDecimal creditMinimumPayment,
            BigDecimal creditNoInterestPayment,
            BigDecimal creditCurrentPaymentDue) {
        OperationalAccountKind kind = accountKind != null ? accountKind : OperationalAccountKind.BANK;
        if (kind == OperationalAccountKind.BANK && isBlank(bankName)) {
            throw new IllegalArgumentException("El nombre del banco es obligatorio.");
        }
        if (kind != OperationalAccountKind.BANK && isBlank(bankName) && isBlank(aliasValue)) {
            throw new IllegalArgumentException("El nombre o referencia de la cuenta es obligatorio.");
        }
        String sourceName = isBlank(bankName) ? aliasValue.trim() : bankName.trim();
        String alias = isBlank(aliasValue) ? sourceName : aliasValue.trim();
        String currency = isBlank(currencyValue) ? "MXN" : currencyValue.trim().toUpperCase();
        if (currency.length() != 3) {
            throw new IllegalArgumentException("La moneda debe tener 3 caracteres, por ejemplo MXN.");
        }
        String last4 = clean(accountLast4);
        if (kind == OperationalAccountKind.BANK && last4 != null && !last4.matches("\\d{4}")) {
            throw new IllegalArgumentException("Los ultimos digitos de la cuenta bancaria deben ser 4 numeros.");
        }
        if (kind != OperationalAccountKind.BANK) {
            last4 = null;
        }
        BankAccountType type = accountType != null ? accountType : BankAccountType.DEBIT;

        account.setBankName(sourceName);
        account.setAlias(alias);
        account.setAccountLast4(last4);
        account.setCurrency(currency);
        account.setAccountType(type);
        account.setAccountKind(kind);
        account.setAccountCode(accountCode(kind, type));
        applyCreditMetadata(
                account,
                creditCutoffDate,
                creditPaymentDueDate,
                creditLimit,
                creditCurrentAmount,
                creditMinimumPayment,
                creditNoInterestPayment,
                creditCurrentPaymentDue
        );
    }

    private void applyCreditMetadata(
            BankAccount account,
            LocalDate creditCutoffDate,
            LocalDate creditPaymentDueDate,
            BigDecimal creditLimit,
            BigDecimal creditCurrentAmount,
            BigDecimal creditMinimumPayment,
            BigDecimal creditNoInterestPayment,
            BigDecimal creditCurrentPaymentDue) {
        if (account.getAccountType() != BankAccountType.CREDIT) {
            account.setCreditCutoffDate(null);
            account.setCreditPaymentDueDate(null);
            account.setCreditLimit(null);
            account.setCreditCurrentAmount(null);
            account.setCreditMinimumPayment(null);
            account.setCreditNoInterestPayment(null);
            account.setCreditCurrentPaymentDue(null);
            return;
        }

        account.setCreditCutoffDate(creditCutoffDate);
        account.setCreditPaymentDueDate(creditPaymentDueDate);
        account.setCreditLimit(optionalNonNegative(creditLimit, "El credito de la tarjeta no puede ser negativo."));
        account.setCreditCurrentAmount(optionalNonNegative(creditCurrentAmount, "El monto actual no puede ser negativo."));
        account.setCreditMinimumPayment(optionalNonNegative(creditMinimumPayment, "El monto minimo no puede ser negativo."));
        account.setCreditNoInterestPayment(optionalNonNegative(creditNoInterestPayment, "El monto para no generar intereses no puede ser negativo."));
        account.setCreditCurrentPaymentDue(optionalNonNegative(creditCurrentPaymentDue, "El monto actual a pagar no puede ser negativo."));
    }

    private void addDebitLine(List<JournalLine> lines, String accountCode, String accountName, BigDecimal amount) {
        if (amount.signum() <= 0) {
            return;
        }
        lines.add(JournalLine.builder()
                .id(UUID.randomUUID())
                .accountCode(accountCode)
                .accountName(accountName)
                .debit(amount)
                .credit(BigDecimal.ZERO)
                .build());
    }

    private void addBankOpeningLine(List<JournalLine> lines, BankAccount account) {
        if (account.getOpeningBalance().signum() <= 0) {
            return;
        }
        JournalLine.JournalLineBuilder builder = JournalLine.builder()
                .id(UUID.randomUUID())
                .bankAccountId(account.getId())
                .accountCode(accountCode(account.getAccountKind(), account.getAccountType()))
                .accountName(accountName(account.getAccountKind(), account.getAccountType()) + " - " + account.displayName());
        if (account.getAccountType() == BankAccountType.CREDIT) {
            builder.debit(BigDecimal.ZERO).credit(account.getOpeningBalance());
        } else {
            builder.debit(account.getOpeningBalance()).credit(BigDecimal.ZERO);
        }
        lines.add(builder.build());
    }

    private void addCapitalBalancingLine(List<JournalLine> lines, String accountNameSuffix) {
        BigDecimal debitTotal = lines.stream().map(JournalLine::getDebit).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal creditTotal = lines.stream().map(JournalLine::getCredit).reduce(BigDecimal.ZERO, BigDecimal::add);
        int comparison = debitTotal.compareTo(creditTotal);
        if (comparison == 0) {
            return;
        }
        BigDecimal difference = debitTotal.subtract(creditTotal).abs();
        lines.add(JournalLine.builder()
                .id(UUID.randomUUID())
                .accountCode(CAPITAL_INICIAL)
                .accountName(accountNameSuffix == null ? CAPITAL_INICIAL_NOMBRE : accountNameSuffix)
                .debit(comparison < 0 ? difference : BigDecimal.ZERO)
                .credit(comparison > 0 ? difference : BigDecimal.ZERO)
                .build());
    }

    private JournalEntry createBankAccountAdjustmentEntry(
            UUID clinicId,
            BankAccount account,
            BigDecimal balanceDelta,
            LocalDate entryDate,
            String description,
            String sourceEventType,
            String notes) {
        List<JournalLine> lines = new ArrayList<>();
        BigDecimal amount = balanceDelta.abs();
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("El monto de ajuste debe ser mayor a cero.");
        }

        boolean increasesBalance = balanceDelta.signum() > 0;
        boolean creditAccount = account.getAccountType() == BankAccountType.CREDIT;
        JournalLine bankLine = JournalLine.builder()
                .id(UUID.randomUUID())
                .bankAccountId(account.getId())
                .accountCode(accountCode(account.getAccountKind(), account.getAccountType()))
                .accountName(accountName(account.getAccountKind(), account.getAccountType()) + " - " + account.displayName())
                .debit(creditAccount
                        ? (increasesBalance ? BigDecimal.ZERO : amount)
                        : (increasesBalance ? amount : BigDecimal.ZERO))
                .credit(creditAccount
                        ? (increasesBalance ? amount : BigDecimal.ZERO)
                        : (increasesBalance ? BigDecimal.ZERO : amount))
                .build();
        lines.add(bankLine);
        addCapitalBalancingLine(lines, "Ajuste contra capital inicial");

        String fullDescription = clean(notes) == null ? description : description + " - " + clean(notes);
        JournalEntry entry = JournalEntry.builder()
                .id(UUID.randomUUID())
                .clinicId(clinicId)
                .description(fullDescription)
                .entryDate(entryDate)
                .sourceEventType(sourceEventType)
                .sourceEventId(UUID.randomUUID())
                .createdAt(now())
                .lines(lines)
                .build();
        entry.validateBalanced();
        return journalEntryRepository.save(entry);
    }

    private BankAccount getBankAccount(UUID clinicId, UUID bankAccountId) {
        if (clinicId == null) {
            throw new IllegalArgumentException("La clinica es obligatoria.");
        }
        return bankAccountRepository.findByIdAndClinicId(bankAccountId, clinicId)
                .orElseThrow(() -> new IllegalArgumentException("La cuenta bancaria no existe en esta clinica."));
    }

    private BigDecimal currentBalance(UUID clinicId, BankAccount account) {
        return journalEntryRepository.findByClinicIdAndBankAccountId(clinicId, account.getId()).stream()
                .flatMap(entry -> entry.getLines().stream())
                .filter(line -> account.getId().equals(line.getBankAccountId()))
                .map(line -> movementAmount(account, line))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean hasMovements(UUID clinicId, BankAccount account) {
        return journalEntryRepository.findByClinicIdAndBankAccountId(clinicId, account.getId()).stream()
                .flatMap(entry -> entry.getLines().stream())
                .anyMatch(line -> account.getId().equals(line.getBankAccountId()));
    }

    private BigDecimal movementAmount(BankAccount account, JournalLine line) {
        BigDecimal debit = line.getDebit() != null ? line.getDebit() : BigDecimal.ZERO;
        BigDecimal credit = line.getCredit() != null ? line.getCredit() : BigDecimal.ZERO;
        return account.getAccountType() == BankAccountType.CREDIT
                ? credit.subtract(debit)
                : debit.subtract(credit);
    }

    private String accountCode(OperationalAccountKind kind, BankAccountType type) {
        if (type == BankAccountType.CREDIT) {
            return CREDITO_BANCARIO;
        }
        OperationalAccountKind normalizedKind = kind != null ? kind : OperationalAccountKind.BANK;
        return switch (normalizedKind) {
            case PETTY_CASH -> CAJA_CHICA;
            case RESERVE -> FONDOS_ESPECIFICOS;
            case OTHER -> OTRAS_DISPONIBILIDADES;
            case BANK -> BANCOS;
        };
    }

    private String accountName(OperationalAccountKind kind, BankAccountType type) {
        if (type == BankAccountType.CREDIT) {
            return CREDITO_BANCARIO_NOMBRE;
        }
        OperationalAccountKind normalizedKind = kind != null ? kind : OperationalAccountKind.BANK;
        return switch (normalizedKind) {
            case PETTY_CASH -> CAJA_CHICA_NOMBRE;
            case RESERVE -> FONDOS_ESPECIFICOS_NOMBRE;
            case OTHER -> OTRAS_DISPONIBILIDADES_NOMBRE;
            case BANK -> BANCOS_NOMBRE;
        };
    }

    private void ensureTransferable(BankAccount account) {
        if (!account.isActive()) {
            throw new IllegalStateException("No se puede transferir desde o hacia una cuenta inactiva.");
        }
        if (account.getAccountType() == BankAccountType.CREDIT) {
            throw new IllegalArgumentException("Las transferencias solo pueden usar cuentas de disponibilidad.");
        }
    }

    private void validateClinicAndCommand(UUID clinicId, CreateBankAccountCommand command) {
        if (clinicId == null) {
            throw new IllegalArgumentException("La clinica es obligatoria.");
        }
        if (command == null) {
            throw new IllegalArgumentException("La cuenta bancaria es obligatoria.");
        }
    }

    private BigDecimal nonNegative(BigDecimal amount, String message) {
        BigDecimal value = amount != null ? amount : BigDecimal.ZERO;
        if (value.signum() < 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private BigDecimal optionalNonNegative(BigDecimal amount, String message) {
        if (amount == null) {
            return null;
        }
        if (amount.signum() < 0) {
            throw new IllegalArgumentException(message);
        }
        return amount;
    }

    private String buildDescription(String notes) {
        String description = "Poliza de saldos iniciales del consultorio";
        String cleanNotes = clean(notes);
        return cleanNotes == null ? description : description + " - " + cleanNotes;
    }

    private boolean isBlank(BigDecimal value) {
        return value == null || value.compareTo(BigDecimal.ZERO) == 0;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String clean(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static LocalDateTime now() {
        return LocalDateTime.now(ZoneId.systemDefault());
    }
}
