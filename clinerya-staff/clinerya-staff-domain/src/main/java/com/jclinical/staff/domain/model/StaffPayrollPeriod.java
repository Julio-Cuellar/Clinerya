package com.jclinical.staff.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffPayrollPeriod {
    private UUID id;
    private UUID clinicId;
    private String name;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private StaffPayrollPeriodStatus status;
    private StaffPayrollPaymentStatus paymentStatus;
    private BigDecimal grossAmount;
    private BigDecimal netAmount;
    private LocalDateTime createdAt;
    private LocalDateTime closedAt;
    private LocalDateTime paidAt;
    private UUID paymentAccountId;
    private UUID paymentJournalEntryId;
}
