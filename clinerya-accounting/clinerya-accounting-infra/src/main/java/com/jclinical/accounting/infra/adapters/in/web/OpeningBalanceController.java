package com.jclinical.accounting.infra.adapters.in.web;

import com.jclinical.accounting.domain.model.BankAccount;
import com.jclinical.accounting.domain.model.CreditAccountAlert;
import com.jclinical.accounting.domain.model.BankAccountType;
import com.jclinical.accounting.domain.model.JournalEntry;
import com.jclinical.accounting.domain.model.OpeningBalanceSetup;
import com.jclinical.accounting.domain.model.OperationalAccountKind;
import com.jclinical.accounting.domain.ports.in.ManageOpeningBalancesUseCase;
import com.jclinical.accounting.infra.adapters.in.web.dto.BankAccountOpeningRequest;
import com.jclinical.accounting.infra.adapters.in.web.dto.BankAccountMovementResponse;
import com.jclinical.accounting.infra.adapters.in.web.dto.BankAccountResponse;
import com.jclinical.accounting.infra.adapters.in.web.dto.CorrectBankAccountBalanceRequest;
import com.jclinical.accounting.infra.adapters.in.web.dto.CreateBankAccountRequest;
import com.jclinical.accounting.infra.adapters.in.web.dto.CreateOpeningBalanceSetupRequest;
import com.jclinical.accounting.infra.adapters.in.web.dto.CreditAccountAlertResponse;
import com.jclinical.accounting.infra.adapters.in.web.dto.DeactivateBankAccountRequest;
import com.jclinical.accounting.infra.adapters.in.web.dto.JournalEntryResponse;
import com.jclinical.accounting.infra.adapters.in.web.dto.JournalLineResponse;
import com.jclinical.accounting.infra.adapters.in.web.dto.OpeningBalanceSetupResponse;
import com.jclinical.accounting.infra.adapters.in.web.dto.UpdateBankAccountRequest;
import com.jclinical.accounting.infra.adapters.in.web.dto.TransferFundsRequest;
import com.jclinical.users.infra.security.CurrentUserResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clinics/{clinicId}/accounting")
@RequiredArgsConstructor
public class OpeningBalanceController {

    private final ManageOpeningBalancesUseCase openingBalancesUseCase;
    private final CurrentUserResolver currentUserResolver;

    @GetMapping("/opening-balances")
    public ResponseEntity<OpeningBalanceSetupResponse> getOpeningBalances(@PathVariable UUID clinicId) {
        return ResponseEntity.ok(openingBalancesUseCase.getOpeningBalances(currentUserResolver.getCurrentUserId(), clinicId)
                .map(this::toResponse)
                .orElse(null));
    }

    @PostMapping("/opening-balances")
    public ResponseEntity<OpeningBalanceSetupResponse> createOpeningBalances(
            @PathVariable UUID clinicId,
            @RequestBody CreateOpeningBalanceSetupRequest request) {
        OpeningBalanceSetup created = openingBalancesUseCase.configureOpeningBalances(currentUserResolver.getCurrentUserId(), clinicId, toCommand(request));
        return ResponseEntity.ok(toResponse(created));
    }

    @GetMapping({"/bank-accounts", "/operational-accounts"})
    public ResponseEntity<List<BankAccountResponse>> listBankAccounts(@PathVariable UUID clinicId) {
        return ResponseEntity.ok(openingBalancesUseCase.listBankAccounts(clinicId).stream()
                .map(this::toResponse)
                .toList());
    }

    @GetMapping("/credit-alerts")
    public ResponseEntity<List<CreditAccountAlertResponse>> getCreditAlerts(
            @PathVariable UUID clinicId,
            @RequestParam(defaultValue = "7") int withinDays) {
        return ResponseEntity.ok(openingBalancesUseCase
                .listCreditAccountAlerts(currentUserResolver.getCurrentUserId(), clinicId, java.time.LocalDate.now(), withinDays)
                .stream()
                .map(this::toCreditAlertResponse)
                .toList());
    }

    @PostMapping({"/bank-accounts", "/operational-accounts"})
    public ResponseEntity<BankAccountResponse> createBankAccount(
            @PathVariable UUID clinicId,
            @RequestBody CreateBankAccountRequest request) {
        BankAccount created = openingBalancesUseCase.createBankAccount(
                currentUserResolver.getCurrentUserId(),
                clinicId,
                new ManageOpeningBalancesUseCase.CreateBankAccountCommand(
                        request.bankName(),
                        request.alias(),
                        request.accountLast4(),
                        request.currency(),
                        request.openingBalance(),
                        toAccountType(request.accountType()),
                        toAccountKind(request.accountKind()),
                        request.openingDate(),
                        request.creditCutoffDate(),
                        request.creditPaymentDueDate(),
                        request.creditLimit(),
                        request.creditCurrentAmount(),
                        request.creditMinimumPayment(),
                        request.creditNoInterestPayment(),
                        request.creditCurrentPaymentDue(),
                        request.notes()
                )
        );
        return ResponseEntity.ok(toResponse(created));
    }

    @PutMapping("/bank-accounts/{bankAccountId}")
    public ResponseEntity<BankAccountResponse> updateBankAccount(
            @PathVariable UUID clinicId,
            @PathVariable UUID bankAccountId,
            @RequestBody UpdateBankAccountRequest request) {
        BankAccount updated = openingBalancesUseCase.updateBankAccount(
                currentUserResolver.getCurrentUserId(),
                clinicId,
                bankAccountId,
                new ManageOpeningBalancesUseCase.UpdateBankAccountCommand(
                        request.bankName(),
                        request.alias(),
                        request.accountLast4(),
                        request.currency(),
                        toOptionalAccountType(request.accountType()),
                        toOptionalAccountKind(request.accountKind()),
                        request.creditCutoffDate(),
                        request.creditPaymentDueDate(),
                        request.creditLimit(),
                        request.creditCurrentAmount(),
                        request.creditMinimumPayment(),
                        request.creditNoInterestPayment(),
                        request.creditCurrentPaymentDue(),
                        request.reason()
                )
        );
        return ResponseEntity.ok(toResponse(updated));
    }

    @PostMapping({"/bank-accounts/{bankAccountId}/corrections", "/operational-accounts/{bankAccountId}/corrections"})
    public ResponseEntity<JournalEntryResponse> correctBankAccountBalance(
            @PathVariable UUID clinicId,
            @PathVariable UUID bankAccountId,
            @RequestBody CorrectBankAccountBalanceRequest request) {
        JournalEntry entry = openingBalancesUseCase.correctBankAccountBalance(
                currentUserResolver.getCurrentUserId(),
                clinicId,
                bankAccountId,
                new ManageOpeningBalancesUseCase.CorrectBankAccountBalanceCommand(
                        request.entryDate(),
                        request.correctedBalance(),
                        request.reason()
                )
        );
        return ResponseEntity.ok(toResponse(entry));
    }

    @PostMapping({"/bank-accounts/{bankAccountId}/deactivate", "/operational-accounts/{bankAccountId}/deactivate"})
    public ResponseEntity<BankAccountResponse> deactivateBankAccount(
            @PathVariable UUID clinicId,
            @PathVariable UUID bankAccountId,
            @RequestBody DeactivateBankAccountRequest request) {
        BankAccount account = openingBalancesUseCase.deactivateBankAccount(
                currentUserResolver.getCurrentUserId(),
                clinicId,
                bankAccountId,
                new ManageOpeningBalancesUseCase.DeactivateBankAccountCommand(
                        request.entryDate(),
                        request.reason()
                )
        );
        return ResponseEntity.ok(toResponse(account));
    }

    @GetMapping({"/bank-accounts/{bankAccountId}/movements", "/operational-accounts/{bankAccountId}/movements"})
    public ResponseEntity<List<BankAccountMovementResponse>> listBankAccountMovements(
            @PathVariable UUID clinicId,
            @PathVariable UUID bankAccountId) {
        return ResponseEntity.ok(openingBalancesUseCase.listBankAccountMovements(currentUserResolver.getCurrentUserId(), clinicId, bankAccountId).stream()
                .map(this::toResponse)
                .toList());
    }

    @PostMapping("/account-transfers")
    public ResponseEntity<JournalEntryResponse> transferFunds(
            @PathVariable UUID clinicId,
            @RequestBody TransferFundsRequest request) {
        JournalEntry entry = openingBalancesUseCase.transferFunds(
                currentUserResolver.getCurrentUserId(),
                clinicId,
                new ManageOpeningBalancesUseCase.TransferFundsCommand(
                        request.sourceAccountId(),
                        request.destinationAccountId(),
                        request.amount(),
                        request.entryDate(),
                        request.description()
                )
        );
        return ResponseEntity.ok(toResponse(entry));
    }

    private ManageOpeningBalancesUseCase.ConfigureOpeningBalancesCommand toCommand(CreateOpeningBalanceSetupRequest request) {
        List<ManageOpeningBalancesUseCase.BankAccountOpeningCommand> bankAccounts = request.bankAccounts() == null
                ? List.of()
                : request.bankAccounts().stream().map(this::toCommand).toList();
        return new ManageOpeningBalancesUseCase.ConfigureOpeningBalancesCommand(
                request.entryDate(),
                request.cashOpeningAmount(),
                request.inventoryOpeningAmount(),
                bankAccounts,
                request.notes()
        );
    }

    private ManageOpeningBalancesUseCase.BankAccountOpeningCommand toCommand(BankAccountOpeningRequest request) {
        return new ManageOpeningBalancesUseCase.BankAccountOpeningCommand(
                request.bankName(),
                request.alias(),
                request.accountLast4(),
                request.currency(),
                request.openingBalance(),
                toAccountType(request.accountType()),
                toAccountKind(request.accountKind())
        );
    }

    private BankAccountType toAccountType(String accountType) {
        if (accountType == null || accountType.isBlank()) {
            return BankAccountType.DEBIT;
        }
        return BankAccountType.valueOf(accountType.trim().toUpperCase());
    }

    private BankAccountType toOptionalAccountType(String accountType) {
        if (accountType == null || accountType.isBlank()) {
            return null;
        }
        return BankAccountType.valueOf(accountType.trim().toUpperCase());
    }

    private OperationalAccountKind toAccountKind(String accountKind) {
        if (accountKind == null || accountKind.isBlank()) {
            return OperationalAccountKind.BANK;
        }
        return OperationalAccountKind.valueOf(accountKind.trim().toUpperCase());
    }

    private OperationalAccountKind toOptionalAccountKind(String accountKind) {
        if (accountKind == null || accountKind.isBlank()) {
            return null;
        }
        return OperationalAccountKind.valueOf(accountKind.trim().toUpperCase());
    }

    private OpeningBalanceSetupResponse toResponse(OpeningBalanceSetup setup) {
        List<BankAccountResponse> bankAccounts = setup.getBankAccounts() == null
                ? List.of()
                : setup.getBankAccounts().stream().map(this::toResponse).toList();
        return new OpeningBalanceSetupResponse(
                setup.getId(),
                setup.getClinicId(),
                setup.getEntryDate(),
                setup.getCashOpeningAmount(),
                setup.getInventoryOpeningAmount(),
                setup.getTotalOpeningAssets(),
                setup.getCapitalAccountCode(),
                setup.getCapitalAccountName(),
                setup.getJournalEntryId(),
                setup.getNotes(),
                setup.getCreatedAt(),
                bankAccounts
        );
    }

    private BankAccountResponse toResponse(BankAccount account) {
        return new BankAccountResponse(
                account.getId(),
                account.getClinicId(),
                account.getOpeningBalanceSetupId(),
                account.getOpeningJournalEntryId(),
                account.getAccountCode(),
                account.getAccountType() != null ? account.getAccountType().name() : BankAccountType.DEBIT.name(),
                account.getAccountKind() != null ? account.getAccountKind().name() : OperationalAccountKind.BANK.name(),
                account.getBankName(),
                account.getAlias(),
                account.getAccountLast4(),
                account.getCurrency(),
                account.getOpeningBalance(),
                account.getOpeningDate(),
                account.getCreditCutoffDate(),
                account.getCreditPaymentDueDate(),
                account.getCreditLimit(),
                account.getCreditCurrentAmount(),
                account.getCreditMinimumPayment(),
                account.getCreditNoInterestPayment(),
                account.getCreditCurrentPaymentDue(),
                account.isActive(),
                account.getDeactivatedAt(),
                account.getDeactivationReason(),
                account.getLastModifiedAt(),
                account.getLastModificationReason(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }

    private CreditAccountAlertResponse toCreditAlertResponse(CreditAccountAlert alert) {
        return new CreditAccountAlertResponse(
                alert.accountId(),
                alert.accountName(),
                alert.type().name(),
                alert.alertDate(),
                alert.daysRemaining(),
                alert.severity(),
                alert.title(),
                alert.detail()
        );
    }

    private BankAccountMovementResponse toResponse(ManageOpeningBalancesUseCase.BankAccountMovement movement) {
        return new BankAccountMovementResponse(
                movement.journalEntryId(),
                movement.journalLineId(),
                movement.entryDate(),
                movement.description(),
                movement.sourceEventType(),
                movement.debit(),
                movement.credit(),
                movement.movementAmount(),
                movement.balanceAfter()
        );
    }

    private JournalEntryResponse toResponse(JournalEntry entry) {
        return new JournalEntryResponse(
                entry.getId(),
                entry.getClinicId(),
                entry.getDescription(),
                entry.getEntryDate(),
                entry.getSourceEventType(),
                entry.getSourceEventId(),
                entry.getCreatedAt(),
                entry.getLines().stream()
                        .map(line -> new JournalLineResponse(
                                line.getId(),
                                line.getBankAccountId(),
                                line.getAccountCode(),
                                line.getAccountName(),
                                line.getDebit(),
                                line.getCredit()
                        ))
                        .toList(),
                entry.getLines().stream().map(line -> line.getDebit() == null ? java.math.BigDecimal.ZERO : line.getDebit()).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add),
                entry.getLines().stream().map(line -> line.getCredit() == null ? java.math.BigDecimal.ZERO : line.getCredit()).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add)
        );
    }
}
