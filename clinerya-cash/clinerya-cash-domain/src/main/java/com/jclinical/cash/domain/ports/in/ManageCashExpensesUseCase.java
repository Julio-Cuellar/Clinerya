package com.jclinical.cash.domain.ports.in;

import com.jclinical.cash.domain.model.CashExpense;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ManageCashExpensesUseCase {

    CashExpense registerExpense(UUID clinicId, RegisterExpenseCommand command);

    CashExpense getExpense(UUID expenseId, UUID clinicId);

    List<CashExpense> listBySession(UUID cashSessionId, UUID clinicId);

    CashExpense voidExpense(UUID expenseId, UUID clinicId, VoidExpenseCommand command);

    record RegisterExpenseCommand(String concept, BigDecimal amount, UUID createdByStaffId) {}

    record VoidExpenseCommand(UUID staffId, String reason) {}
}
