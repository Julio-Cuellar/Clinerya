package com.jclinical.cash.infra.config;

import com.jclinical.cash.domain.model.CashExpense;
import com.jclinical.cash.domain.ports.in.ManageCashExpensesUseCase;
import com.jclinical.cash.domain.service.CashExpenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Primary
@RequiredArgsConstructor
public class TransactionalCashExpenseUseCase implements ManageCashExpensesUseCase {

    private final CashExpenseService cashExpenseService;

    @Override
    @Transactional
    public CashExpense registerExpense(UUID clinicId, UUID actingUserId, RegisterExpenseCommand command) {
        return cashExpenseService.registerExpense(clinicId, actingUserId, command);
    }

    @Override
    @Transactional(readOnly = true)
    public CashExpense getExpense(UUID expenseId, UUID clinicId, UUID actingUserId) {
        return cashExpenseService.getExpense(expenseId, clinicId, actingUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CashExpense> listBySession(UUID cashSessionId, UUID clinicId, UUID actingUserId) {
        return cashExpenseService.listBySession(cashSessionId, clinicId, actingUserId);
    }

    @Override
    @Transactional
    public CashExpense voidExpense(UUID expenseId, UUID clinicId, UUID actingUserId, VoidExpenseCommand command) {
        return cashExpenseService.voidExpense(expenseId, clinicId, actingUserId, command);
    }
}
