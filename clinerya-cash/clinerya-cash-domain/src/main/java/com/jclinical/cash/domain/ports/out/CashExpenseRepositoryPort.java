package com.jclinical.cash.domain.ports.out;

import com.jclinical.cash.domain.model.CashExpense;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CashExpenseRepositoryPort {

    CashExpense save(CashExpense expense);

    Optional<CashExpense> findByIdAndClinicId(UUID expenseId, UUID clinicId);

    List<CashExpense> findByCashSessionId(UUID cashSessionId, UUID clinicId);

    BigDecimal sumActiveAmountBySession(UUID cashSessionId);
}
