package com.jclinical.cash.domain.ports.in;

import com.jclinical.cash.domain.model.CashExpense;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ManageCashExpensesUseCase {

    CashExpense registerExpense(UUID clinicId, UUID actingUserId, RegisterExpenseCommand command);

    CashExpense getExpense(UUID expenseId, UUID clinicId, UUID actingUserId);

    List<CashExpense> listBySession(UUID cashSessionId, UUID clinicId, UUID actingUserId);

    CashExpense voidExpense(UUID expenseId, UUID clinicId, UUID actingUserId, VoidExpenseCommand command);

    record RegisterExpenseCommand(String concept, BigDecimal amount, UUID createdByStaffId) {}

    record VoidExpenseCommand(UUID staffId, String reason) {}
}
