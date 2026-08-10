package com.jclinical.accounting.infra.adapters.in.web.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record JournalEntryResponse(
        UUID id,
        UUID clinicId,
        String description,
        LocalDate entryDate,
        String sourceEventType,
        UUID sourceEventId,
        LocalDateTime createdAt,
        List<JournalLineResponse> lines,
        BigDecimal totalDebit,
        BigDecimal totalCredit
) {
    public static JournalEntryResponse from(com.jclinical.accounting.domain.model.JournalEntry entry) {
        BigDecimal totalDebit = entry.getLines().stream().map(line -> line.getDebit() == null ? BigDecimal.ZERO : line.getDebit()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredit = entry.getLines().stream().map(line -> line.getCredit() == null ? BigDecimal.ZERO : line.getCredit()).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new JournalEntryResponse(entry.getId(), entry.getClinicId(), entry.getDescription(),
                entry.getEntryDate(), entry.getSourceEventType(), entry.getSourceEventId(), entry.getCreatedAt(),
                entry.getLines().stream().map(line -> new JournalLineResponse(
                        line.getId(), line.getBankAccountId(), line.getAccountCode(), line.getAccountName(),
                        line.getDebit(), line.getCredit())).toList(), totalDebit, totalCredit);
    }
}
