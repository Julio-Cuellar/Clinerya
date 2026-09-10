package com.jclinical.accounting.domain.service;

import com.jclinical.accounting.domain.model.BankAccount;
import com.jclinical.accounting.domain.model.BankAccountType;
import com.jclinical.accounting.domain.model.CreditAccountAlert;
import com.jclinical.accounting.domain.model.JournalEntry;
import com.jclinical.accounting.domain.model.JournalQueryResult;
import com.jclinical.accounting.domain.model.OpeningBalanceSetup;
import com.jclinical.accounting.domain.model.OperationalAccountKind;
import com.jclinical.accounting.domain.ports.in.ManageOpeningBalancesUseCase.BankAccountMovement;
import com.jclinical.accounting.domain.ports.in.ManageOpeningBalancesUseCase.BankAccountOpeningCommand;
import com.jclinical.accounting.domain.ports.in.ManageOpeningBalancesUseCase.ConfigureOpeningBalancesCommand;
import com.jclinical.accounting.domain.ports.in.ManageOpeningBalancesUseCase.CorrectBankAccountBalanceCommand;
import com.jclinical.accounting.domain.ports.in.ManageOpeningBalancesUseCase.CreateBankAccountCommand;
import com.jclinical.accounting.domain.ports.in.ManageOpeningBalancesUseCase.DeactivateBankAccountCommand;
import com.jclinical.accounting.domain.ports.in.ManageOpeningBalancesUseCase.TransferFundsCommand;
import com.jclinical.accounting.domain.ports.in.ManageOpeningBalancesUseCase.UpdateBankAccountCommand;
import com.jclinical.accounting.domain.ports.out.BankAccountRepositoryPort;
import com.jclinical.accounting.domain.ports.out.JournalEntryRepositoryPort;
import com.jclinical.accounting.domain.ports.out.OpeningBalanceSetupRepositoryPort;
import com.jclinical.core.security.StaffPermissionCheckerPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpeningBalanceServiceTest {

    private final InMemorySetupRepository setupRepository = new InMemorySetupRepository();
    private final InMemoryBankAccountRepository bankAccountRepository = new InMemoryBankAccountRepository();
    private final InMemoryJournalEntryRepository journalEntryRepository = new InMemoryJournalEntryRepository();
    private static final UUID ACTING_USER = UUID.randomUUID();
    private OpeningBalanceService service;

    @BeforeEach
    void setUp() {
        service = new OpeningBalanceService(setupRepository, bankAccountRepository, journalEntryRepository,
                (c, u, p) -> true);
    }

    @Test
    void configuresOpeningBalancesWithCashInventoryDebitAndCreditAccounts() {
        UUID clinicId = UUID.randomUUID();
        LocalDate entryDate = LocalDate.of(2026, 8, 10);

        OpeningBalanceSetup setup = service.configureOpeningBalances(ACTING_USER, clinicId, new ConfigureOpeningBalancesCommand(
                entryDate,
                money("5000.00"),
                money("2500.00"),
                List.of(
                        new BankAccountOpeningCommand("BBVA", "Principal", "1234", "mxn", money("12000.00"), BankAccountType.DEBIT, OperationalAccountKind.BANK),
                        new BankAccountOpeningCommand("Nu", "Credito", "9876", "MXN", money("3000.00"), BankAccountType.CREDIT, OperationalAccountKind.BANK),
                        new BankAccountOpeningCommand(null, "Caja chica recepcion", null, "MXN", money("800.00"), BankAccountType.DEBIT, OperationalAccountKind.PETTY_CASH)
                ),
                "  arranque clinica  "));

        assertNotNull(setup.getId());
        assertEquals(clinicId, setup.getClinicId());
        assertEquals(entryDate, setup.getEntryDate());
        assertEquals(money("23300.00"), setup.getTotalOpeningAssets());
        assertEquals("arranque clinica", setup.getNotes());
        assertEquals(3, setup.getBankAccounts().size());

        JournalEntry entry = journalEntryRepository.entries.get(0);
        assertEquals("SaldoInicial", entry.getSourceEventType());
        assertEquals(0, entry.totalDebits().compareTo(entry.totalCredits()));
        assertTrue(entry.getLines().stream().anyMatch(line -> "11200".equals(line.getAccountCode()) && money("12000.00").compareTo(line.getDebit()) == 0));
        assertTrue(entry.getLines().stream().anyMatch(line -> "21200".equals(line.getAccountCode()) && money("3000.00").compareTo(line.getCredit()) == 0));
        assertTrue(setup.getBankAccounts().stream().allMatch(account -> setup.getId().equals(account.getOpeningBalanceSetupId())));
    }

    @Test
    void rejectsInvalidOpeningBalancesAndDuplicateSetup() {
        UUID clinicId = UUID.randomUUID();
        ConfigureOpeningBalancesCommand emptyCommand =
                new ConfigureOpeningBalancesCommand(LocalDate.now(), BigDecimal.ZERO, BigDecimal.ZERO, List.of(), null);

        assertThrows(IllegalArgumentException.class, () -> service.configureOpeningBalances(ACTING_USER, clinicId, emptyCommand));

        service.configureOpeningBalances(ACTING_USER, clinicId, new ConfigureOpeningBalancesCommand(
                LocalDate.now(), money("1.00"), BigDecimal.ZERO, List.of(), null));

        ConfigureOpeningBalancesCommand duplicateCommand =
                new ConfigureOpeningBalancesCommand(LocalDate.now(), money("1.00"), BigDecimal.ZERO, List.of(), null);
        assertThrows(IllegalStateException.class, () -> service.configureOpeningBalances(ACTING_USER, clinicId, duplicateCommand));
    }

    @Test
    void createsUpdatesAndAlertsCreditBankAccounts() {
        UUID clinicId = UUID.randomUUID();
        BankAccount credit = service.createBankAccount(clinicId, new CreateBankAccountCommand(
                "Santander",
                "Tarjeta insumos",
                "4321",
                "mxn",
                money("1500.00"),
                BankAccountType.CREDIT,
                OperationalAccountKind.BANK,
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 12),
                LocalDate.of(2026, 8, 15),
                money("30000.00"),
                money("1500.00"),
                money("300.00"),
                money("1500.00"),
                money("300.00"),
                "saldo inicial tarjeta"));

        assertEquals("MXN", credit.getCurrency());
        assertEquals("21200", credit.getAccountCode());
        assertEquals(1, journalEntryRepository.entries.size());

        List<CreditAccountAlert> alerts = service.listCreditAccountAlerts(ACTING_USER, clinicId, LocalDate.of(2026, 8, 10), 10);

        assertEquals(2, alerts.size());
        assertEquals(CreditAccountAlert.AlertType.CUTOFF, alerts.get(0).type());
        assertEquals(CreditAccountAlert.AlertType.PAYMENT_DUE, alerts.get(1).type());

        BankAccount updated = service.updateBankAccount(ACTING_USER, clinicId, credit.getId(), new UpdateBankAccountCommand(
                "Santander",
                "Tarjeta compras",
                "4321",
                "MXN",
                BankAccountType.CREDIT,
                OperationalAccountKind.BANK,
                LocalDate.of(2026, 8, 13),
                LocalDate.of(2026, 8, 16),
                money("35000.00"),
                money("1200.00"),
                money("250.00"),
                money("1200.00"),
                money("250.00"),
                "actualizacion"));

        assertEquals("Tarjeta compras ****4321", updated.displayName());
        assertEquals("actualizacion", updated.getLastModificationReason());
        assertEquals(money("35000.00"), updated.getCreditLimit());
    }

    @Test
    void correctsBalancesListsMovementsTransfersAndDeactivatesAccounts() {
        UUID clinicId = UUID.randomUUID();
        BankAccount source = service.createBankAccount(clinicId, debitCommand("BBVA", "Principal", "1111", money("1000.00")));
        BankAccount destination = service.createBankAccount(clinicId, debitCommand("Banorte", "Reserva", "2222", money("100.00")));

        JournalEntry correction = service.correctBankAccountBalance(
                ACTING_USER,
                clinicId,
                source.getId(),
                new CorrectBankAccountBalanceCommand(LocalDate.of(2026, 8, 11), money("1300.00"), "corte bancario"));

        assertEquals("CuentaBancariaCorreccion", correction.getSourceEventType());

        JournalEntry transfer = service.transferFunds(ACTING_USER, clinicId, new TransferFundsCommand(
                source.getId(),
                destination.getId(),
                money("250.00"),
                LocalDate.of(2026, 8, 12),
                "fondeo caja"));

        assertEquals("TransferenciaCuentaOperativa", transfer.getSourceEventType());

        List<BankAccountMovement> sourceMovements = service.listBankAccountMovements(ACTING_USER, clinicId, source.getId());
        assertEquals(3, sourceMovements.size());
        assertEquals(money("1050.00"), sourceMovements.get(2).balanceAfter());

        BankAccount inactive = service.deactivateBankAccount(
                ACTING_USER,
                clinicId,
                destination.getId(),
                new DeactivateBankAccountCommand(LocalDate.of(2026, 8, 13), "cuenta reemplazada"));

        assertFalse(inactive.isActive());
        assertEquals("cuenta reemplazada", inactive.getDeactivationReason());
        assertTrue(journalEntryRepository.entries.stream()
                .anyMatch(entry -> "CuentaBancariaBaja".equals(entry.getSourceEventType())));
    }

    @Test
    void rejectsInvalidBankAccountChangesAndTransfers() {
        UUID clinicId = UUID.randomUUID();
        CreateBankAccountCommand invalidCurrency =
                debitCommand("BBVA", "Principal", "1111", BigDecimal.ZERO, "PESOS");

        assertThrows(IllegalArgumentException.class, () -> service.createBankAccount(clinicId, invalidCurrency));

        BankAccount source = service.createBankAccount(clinicId, debitCommand("BBVA", "Principal", "1111", money("100.00")));
        BankAccount usd = service.createBankAccount(clinicId, debitCommand("Chase", "USD", "3333", money("100.00"), "USD"));

        TransferFundsCommand sameAccountCommand =
                new TransferFundsCommand(source.getId(), source.getId(), money("1.00"), LocalDate.now(), null);
        assertThrows(IllegalArgumentException.class, () -> service.transferFunds(ACTING_USER, clinicId, sameAccountCommand));

        TransferFundsCommand currencyMismatchCommand =
                new TransferFundsCommand(source.getId(), usd.getId(), money("1.00"), LocalDate.now(), null);
        assertThrows(IllegalArgumentException.class, () -> service.transferFunds(ACTING_USER, clinicId, currencyMismatchCommand));

        UpdateBankAccountCommand typeChangeCommand = new UpdateBankAccountCommand(
                "BBVA", "Principal", "1111", "MXN", BankAccountType.CREDIT, OperationalAccountKind.BANK,
                null, null, null, null, null, null, null, "cambio tipo");
        assertThrows(IllegalStateException.class, () -> service.updateBankAccount(ACTING_USER, clinicId, source.getId(), typeChangeCommand));
    }

    private static CreateBankAccountCommand debitCommand(String bankName, String alias, String last4, BigDecimal openingBalance) {
        return debitCommand(bankName, alias, last4, openingBalance, "MXN");
    }

    private static CreateBankAccountCommand debitCommand(String bankName, String alias, String last4, BigDecimal openingBalance, String currency) {
        return new CreateBankAccountCommand(
                bankName, alias, last4, currency, openingBalance, BankAccountType.DEBIT, OperationalAccountKind.BANK,
                LocalDate.of(2026, 8, 10), null, null, null, null, null, null, null, null);
    }

    private static BigDecimal money(String value) {
        return new BigDecimal(value);
    }

    private static final class InMemorySetupRepository implements OpeningBalanceSetupRepositoryPort {
        private final List<OpeningBalanceSetup> setups = new ArrayList<>();

        @Override
        public OpeningBalanceSetup save(OpeningBalanceSetup setup) {
            setups.removeIf(existing -> existing.getId().equals(setup.getId()));
            setups.add(setup);
            return setup;
        }

        @Override
        public Optional<OpeningBalanceSetup> findByClinicId(UUID clinicId) {
            return setups.stream().filter(setup -> clinicId.equals(setup.getClinicId())).findFirst();
        }

        @Override
        public boolean existsByClinicId(UUID clinicId) {
            return setups.stream().anyMatch(setup -> clinicId.equals(setup.getClinicId()));
        }
    }

    private static final class InMemoryBankAccountRepository implements BankAccountRepositoryPort {
        private final List<BankAccount> accounts = new ArrayList<>();

        @Override
        public BankAccount save(BankAccount bankAccount) {
            accounts.removeIf(existing -> existing.getId().equals(bankAccount.getId()));
            accounts.add(bankAccount);
            return bankAccount;
        }

        @Override
        public List<BankAccount> saveAll(List<BankAccount> bankAccounts) {
            bankAccounts.forEach(this::save);
            return bankAccounts;
        }

        @Override
        public List<BankAccount> findByClinicId(UUID clinicId) {
            return accounts.stream()
                    .filter(account -> clinicId.equals(account.getClinicId()))
                    .toList();
        }

        @Override
        public Optional<BankAccount> findByIdAndClinicId(UUID bankAccountId, UUID clinicId) {
            return accounts.stream()
                    .filter(account -> bankAccountId.equals(account.getId()))
                    .filter(account -> clinicId.equals(account.getClinicId()))
                    .findFirst();
        }
    }

    private static final class InMemoryJournalEntryRepository implements JournalEntryRepositoryPort {
        private final List<JournalEntry> entries = new ArrayList<>();

        @Override
        public JournalEntry save(JournalEntry entry) {
            entries.removeIf(existing -> existing.getId().equals(entry.getId()));
            entries.add(entry);
            return entry;
        }

        @Override
        public boolean existsBySourceEventId(UUID sourceEventId) {
            return entries.stream().anyMatch(entry -> sourceEventId.equals(entry.getSourceEventId()));
        }

        @Override
        public List<JournalEntry> findByClinicId(UUID clinicId) {
            return entries.stream().filter(entry -> clinicId.equals(entry.getClinicId())).toList();
        }

        @Override
        public List<JournalEntry> findByClinicIdAndEntryDateBetween(UUID clinicId, LocalDate from, LocalDate to) {
            return findByClinicId(clinicId).stream()
                    .filter(entry -> !entry.getEntryDate().isBefore(from) && !entry.getEntryDate().isAfter(to))
                    .toList();
        }

        @Override
        public List<JournalEntry> findByClinicIdAndBankAccountId(UUID clinicId, UUID bankAccountId) {
            return findByClinicId(clinicId).stream()
                    .filter(entry -> entry.getLines().stream()
                            .anyMatch(line -> bankAccountId.equals(line.getBankAccountId())))
                    .sorted(Comparator.comparing(JournalEntry::getEntryDate))
                    .toList();
        }

        @Override
        public JournalQueryResult queryByClinic(UUID clinicId, LocalDate from, LocalDate to, String search, String sourceEventType, int page, int size) {
            List<JournalEntry> clinicEntries = findByClinicId(clinicId);
            return new JournalQueryResult(
                    clinicEntries,
                    clinicEntries.size(),
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    page,
                    size,
                    1);
        }
    }
}
