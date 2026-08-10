package com.jclinical.accounting.infra.adapters.in.web.dto;

import com.jclinical.accounting.domain.model.WasteReport;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record WasteReportResponse(
        LocalDate from,
        LocalDate to,
        BigDecimal totalAmount,
        long totalEvents,
        List<LineResponse> lines) {

    public static WasteReportResponse from(WasteReport report) {
        return new WasteReportResponse(report.from(), report.to(), report.totalAmount(), report.totalEvents(),
                report.lines().stream().map(LineResponse::from).toList());
    }

    public record LineResponse(
            UUID journalEntryId,
            UUID sourceEventId,
            LocalDate date,
            String description,
            BigDecimal amount) {
        private static LineResponse from(WasteReport.Line line) {
            return new LineResponse(line.journalEntryId(), line.sourceEventId(), line.date(), line.description(), line.amount());
        }
    }
}
