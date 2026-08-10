package com.jclinical.cash.infra.adapters.out.persistence;

import com.jclinical.cash.domain.model.CashExpense;
import com.jclinical.cash.domain.model.CashExpenseStatus;
import com.jclinical.cash.domain.ports.out.CashExpenseRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SqlCashExpenseRepository implements CashExpenseRepositoryPort {

    private final SpringDataCashExpenseRepository springRepository;
    private final CashExpenseMapper mapper;

    @Override
    public CashExpense save(CashExpense expense) {
        CashExpenseEntity entity = mapper.toEntity(expense);
        CashExpenseEntity saved = springRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<CashExpense> findByIdAndClinicId(UUID expenseId, UUID clinicId) {
        return springRepository.findByIdAndClinicId(expenseId, clinicId).map(mapper::toDomain);
    }

    @Override
    public List<CashExpense> findByCashSessionId(UUID cashSessionId, UUID clinicId) {
        return springRepository.findByCashSessionIdAndClinicId(cashSessionId, clinicId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public BigDecimal sumActiveAmountBySession(UUID cashSessionId) {
        BigDecimal sum = springRepository.sumActiveAmountBySession(cashSessionId, CashExpenseStatus.ACTIVE);
        return sum != null ? sum : BigDecimal.ZERO;
    }
}
