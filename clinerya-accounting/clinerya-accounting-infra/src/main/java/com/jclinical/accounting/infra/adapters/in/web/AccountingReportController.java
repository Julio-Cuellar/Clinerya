package com.jclinical.accounting.infra.adapters.in.web;

import com.jclinical.accounting.domain.ports.in.GenerateAccountingReportsUseCase;
import com.jclinical.accounting.infra.adapters.in.web.dto.IncomeStatementResponse;
import com.jclinical.accounting.infra.adapters.in.web.dto.TrialBalanceResponse;
import com.jclinical.accounting.infra.adapters.in.web.dto.WasteReportResponse;
import com.jclinical.users.infra.security.CurrentUserResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clinics/{clinicId}/accounting/reports")
@RequiredArgsConstructor
public class AccountingReportController {

    private final GenerateAccountingReportsUseCase reportsUseCase;
    private final CurrentUserResolver currentUserResolver;

    @GetMapping("/income-statement")
    public ResponseEntity<IncomeStatementResponse> incomeStatement(
            @PathVariable UUID clinicId,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            @RequestParam(defaultValue = "true") boolean compare) {
        if (from.isAfter(to)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La fecha inicial no puede ser posterior a la fecha final.");
        }
        return ResponseEntity.ok(IncomeStatementResponse.from(
                reportsUseCase.generateIncomeStatement(currentUserResolver.getCurrentUserId(), clinicId, from, to, compare)));
    }

    @GetMapping("/trial-balance")
    public ResponseEntity<TrialBalanceResponse> trialBalance(
            @PathVariable UUID clinicId,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to) {
        if (from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La fecha inicial no puede ser posterior a la fecha final.");
        }
        return ResponseEntity.ok(TrialBalanceResponse.from(
                reportsUseCase.generateTrialBalance(currentUserResolver.getCurrentUserId(), clinicId, from, to)));
    }

    @GetMapping("/waste")
    public ResponseEntity<WasteReportResponse> waste(
            @PathVariable UUID clinicId,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to) {
        if (from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La fecha inicial no puede ser posterior a la fecha final.");
        }
        return ResponseEntity.ok(WasteReportResponse.from(
                reportsUseCase.generateWasteReport(currentUserResolver.getCurrentUserId(), clinicId, from, to)));
    }
}
