package com.jclinical.accounting.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalEntry {
    private UUID id;
    private UUID clinicId;
    private String description;
    private LocalDate entryDate;
    private String sourceEventType;
    private UUID sourceEventId;
    @Builder.Default
    private List<JournalLine> lines = new ArrayList<>();
    private LocalDateTime createdAt;

    public BigDecimal totalDebits() {
        return lines.stream().map(JournalLine::getDebit).filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal totalCredits() {
        return lines.stream().map(JournalLine::getCredit).filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void validateBalanced() {
        if (totalDebits().compareTo(totalCredits()) != 0) {
            throw new IllegalStateException(
                    "La póliza no cuadra: débitos (" + totalDebits() + ") distintos de créditos (" + totalCredits() + ").");
        }
    }
}
