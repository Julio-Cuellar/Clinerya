package com.jclinical.accounting.infra.config;

import com.jclinical.accounting.domain.model.IncomeStatementReport;
import com.jclinical.accounting.domain.model.TrialBalanceReport;
import com.jclinical.accounting.domain.model.WasteReport;
import com.jclinical.accounting.domain.ports.in.GenerateAccountingReportsUseCase;
import com.jclinical.accounting.domain.service.IncomeStatementService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@Primary
@RequiredArgsConstructor
public class TransactionalAccountingReportUseCase implements GenerateAccountingReportsUseCase {

    private final IncomeStatementService incomeStatementService;

    @Override
    @Transactional(readOnly = true)
    public IncomeStatementReport generateIncomeStatement(
            UUID actingUserId,
            UUID clinicId,
            LocalDate from,
            LocalDate to,
            boolean includeComparison) {
        return incomeStatementService.generateIncomeStatement(actingUserId, clinicId, from, to, includeComparison);
    }

    @Override
    @Transactional(readOnly = true)
    public TrialBalanceReport generateTrialBalance(UUID actingUserId, UUID clinicId, LocalDate from, LocalDate to) {
        return incomeStatementService.generateTrialBalance(actingUserId, clinicId, from, to);
    }

    @Override
    @Transactional(readOnly = true)
    public WasteReport generateWasteReport(UUID actingUserId, UUID clinicId, LocalDate from, LocalDate to) {
        return incomeStatementService.generateWasteReport(actingUserId, clinicId, from, to);
    }
}
