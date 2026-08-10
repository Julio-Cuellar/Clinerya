package com.jclinical.accounting.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record WasteReport(
        LocalDate from,
        LocalDate to,
        BigDecimal totalAmount,
        long totalEvents,
        List<Line> lines) {

    public record Line(
            UUID journalEntryId,
            UUID sourceEventId,
            LocalDate date,
            String description,
            BigDecimal amount) {
    }
}
