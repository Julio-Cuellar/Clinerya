package com.jclinical.cash.infra.adapters.out.persistence;

import com.jclinical.cash.domain.model.CashExpense;

public interface CashExpenseMapper {

    CashExpenseEntity toEntity(CashExpense domain);

    CashExpense toDomain(CashExpenseEntity entity);
}
