package com.jclinical.accounting.infra.adapters.in.web.dto;

import com.jclinical.accounting.domain.model.JournalQueryResult;

import java.math.BigDecimal;
import java.util.List;

public record JournalQueryResponse(
        List<JournalEntryResponse> entries,
        long totalEntries,
        BigDecimal totalDebit,
        BigDecimal totalCredit,
        int page,
        int size,
        int totalPages) {

    public static JournalQueryResponse from(JournalQueryResult result) {
        return new JournalQueryResponse(result.entries().stream().map(JournalEntryResponse::from).toList(),
                result.totalEntries(), result.totalDebit(), result.totalCredit(),
                result.page(), result.size(), result.totalPages());
    }
}
