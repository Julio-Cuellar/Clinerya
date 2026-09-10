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
    public CashExpense registerExpense(UUID actingUserId, UUID clinicId, RegisterExpenseCommand command) {
        return cashExpenseService.registerExpense(actingUserId, clinicId, command);
    }

    @Override
    @Transactional(readOnly = true)
    public CashExpense getExpense(UUID actingUserId, UUID expenseId, UUID clinicId) {
        return cashExpenseService.getExpense(actingUserId, expenseId, clinicId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CashExpense> listBySession(UUID actingUserId, UUID cashSessionId, UUID clinicId) {
        return cashExpenseService.listBySession(actingUserId, cashSessionId, clinicId);
    }

    @Override
    @Transactional
    public CashExpense voidExpense(UUID actingUserId, UUID expenseId, UUID clinicId, VoidExpenseCommand command) {
        return cashExpenseService.voidExpense(actingUserId, expenseId, clinicId, command);
    }
}
