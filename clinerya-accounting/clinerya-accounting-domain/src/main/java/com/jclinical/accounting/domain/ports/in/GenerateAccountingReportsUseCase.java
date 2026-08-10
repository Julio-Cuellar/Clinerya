package com.jclinical.accounting.domain.ports.in;

import com.jclinical.accounting.domain.model.IncomeStatementReport;
import com.jclinical.accounting.domain.model.TrialBalanceReport;
import com.jclinical.accounting.domain.model.WasteReport;

import java.time.LocalDate;
import java.util.UUID;

public interface GenerateAccountingReportsUseCase {

    IncomeStatementReport generateIncomeStatement(
            UUID clinicId,
            LocalDate from,
            LocalDate to,
            boolean includeComparison);

    TrialBalanceReport generateTrialBalance(UUID clinicId, LocalDate from, LocalDate to);

    WasteReport generateWasteReport(UUID clinicId, LocalDate from, LocalDate to);
}
