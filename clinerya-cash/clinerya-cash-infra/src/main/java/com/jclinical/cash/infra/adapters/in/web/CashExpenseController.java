package com.jclinical.cash.infra.adapters.in.web;

import com.jclinical.cash.domain.model.CashExpense;
import com.jclinical.cash.domain.ports.in.ManageCashExpensesUseCase;
import com.jclinical.cash.domain.ports.in.ManageCashExpensesUseCase.RegisterExpenseCommand;
import com.jclinical.cash.domain.ports.in.ManageCashExpensesUseCase.VoidExpenseCommand;
import com.jclinical.cash.infra.adapters.in.web.dto.CashExpenseResponse;
import com.jclinical.cash.infra.adapters.in.web.dto.RegisterCashExpenseRequest;
import com.jclinical.cash.infra.adapters.in.web.dto.VoidCashExpenseRequest;
import com.jclinical.users.infra.security.CurrentUserResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clinics/{clinicId}/cash-expenses")
@RequiredArgsConstructor
public class CashExpenseController {

    private final ManageCashExpensesUseCase cashExpensesUseCase;
    private final CurrentUserResolver currentUserResolver;

    @PostMapping("")
    public ResponseEntity<CashExpenseResponse> registerExpense(
            @PathVariable UUID clinicId,
            @RequestBody RegisterCashExpenseRequest request) {
        RegisterExpenseCommand command = new RegisterExpenseCommand(
                request.concept(),
                request.amount(),
                request.createdByStaffId()
        );
        CashExpense expense = cashExpensesUseCase.registerExpense(clinicId, currentUserResolver.getCurrentUserId(), command);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(expense));
    }

    @GetMapping("/{expenseId}")
    public ResponseEntity<CashExpenseResponse> getExpense(
            @PathVariable UUID clinicId,
            @PathVariable UUID expenseId) {
        CashExpense expense = cashExpensesUseCase.getExpense(expenseId, clinicId, currentUserResolver.getCurrentUserId());
        return ResponseEntity.ok(toResponse(expense));
    }

    @GetMapping("/by-session/{cashSessionId}")
    public ResponseEntity<List<CashExpenseResponse>> listBySession(
            @PathVariable UUID clinicId,
            @PathVariable UUID cashSessionId) {
        List<CashExpenseResponse> responses = cashExpensesUseCase.listBySession(cashSessionId, clinicId, currentUserResolver.getCurrentUserId()).stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/{expenseId}/void")
    public ResponseEntity<CashExpenseResponse> voidExpense(
            @PathVariable UUID clinicId,
            @PathVariable UUID expenseId,
            @RequestBody VoidCashExpenseRequest request) {
        VoidExpenseCommand command = new VoidExpenseCommand(request.staffId(), request.reason());
        CashExpense expense = cashExpensesUseCase.voidExpense(expenseId, clinicId, currentUserResolver.getCurrentUserId(), command);
        return ResponseEntity.ok(toResponse(expense));
    }

    private CashExpenseResponse toResponse(CashExpense expense) {
        return new CashExpenseResponse(
                expense.getId(),
                expense.getClinicId(),
                expense.getCashSessionId(),
                expense.getConcept(),
                expense.getAmount(),
                expense.getCreatedByStaffId(),
                expense.getCreatedAt(),
                expense.getStatus(),
                expense.getVoidedByStaffId(),
                expense.getVoidedAt(),
                expense.getVoidReason()
        );
    }
}
