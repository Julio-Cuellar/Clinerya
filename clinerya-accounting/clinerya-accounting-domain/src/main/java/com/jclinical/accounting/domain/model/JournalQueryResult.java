package com.jclinical.accounting.domain.model;

import java.math.BigDecimal;
import java.util.List;

public record JournalQueryResult(
        List<JournalEntry> entries,
        long totalEntries,
        BigDecimal totalDebit,
        BigDecimal totalCredit,
        int page,
        int size,
        int totalPages) {
}
