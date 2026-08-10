package com.jclinical.accounting.infra.adapters.in.web;

import com.jclinical.accounting.domain.model.JournalEntry;
import com.jclinical.accounting.domain.model.JournalLine;
import com.jclinical.accounting.domain.ports.in.ManageJournalUseCase;
import com.jclinical.accounting.infra.adapters.in.web.dto.JournalEntryResponse;
import com.jclinical.accounting.infra.adapters.in.web.dto.JournalLineResponse;
import com.jclinical.accounting.infra.adapters.in.web.dto.JournalQueryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.jclinical.accounting.infra.adapters.in.web.dto.CreateJournalEntryRequest;

import java.util.List;
import java.util.UUID;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/clinics/{clinicId}/accounting/journal-entries")
@RequiredArgsConstructor
public class JournalEntryController {

    private final ManageJournalUseCase journalUseCase;

    @GetMapping
    public ResponseEntity<List<JournalEntryResponse>> listByClinic(@PathVariable UUID clinicId) {
        List<JournalEntryResponse> responses = journalUseCase.listByClinic(clinicId).stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/query")
    public ResponseEntity<JournalQueryResponse> query(
            @PathVariable UUID clinicId,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sourceEventType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(JournalQueryResponse.from(
                journalUseCase.queryByClinic(clinicId, from, to, search, sourceEventType, page, size)));
    }

    @PostMapping
    public ResponseEntity<JournalEntryResponse> createManual(@PathVariable UUID clinicId, @RequestBody CreateJournalEntryRequest request) {
        List<JournalLine> lines = request.lines().stream()
                .map(line -> JournalLine.builder()
                        .accountCode(line.accountCode())
                        .accountName(line.accountName())
                        .debit(line.debit())
                        .credit(line.credit())
                        .build())
                .toList();

        JournalEntry entry = JournalEntry.builder()
                .description(request.description())
                .entryDate(request.entryDate())
                .lines(lines)
                .build();

        JournalEntry created = journalUseCase.createManualEntry(clinicId, entry);
        return ResponseEntity.ok(toResponse(created));
    }

    private JournalEntryResponse toResponse(JournalEntry entry) {
        return JournalEntryResponse.from(entry);
    }

    private JournalLineResponse toResponse(JournalLine line) {
        return new JournalLineResponse(line.getId(), line.getBankAccountId(), line.getAccountCode(), line.getAccountName(), line.getDebit(), line.getCredit());
    }
}
