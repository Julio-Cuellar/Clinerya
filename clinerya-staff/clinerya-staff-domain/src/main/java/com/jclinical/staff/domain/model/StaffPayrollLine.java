package com.jclinical.staff.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffPayrollLine {
    private UUID id;
    private UUID clinicId;
    private UUID payrollPeriodId;
    private UUID staffId;
    private BigDecimal baseSalary;
    private BigDecimal commissionAmount;
    private BigDecimal bonusAmount;
    private BigDecimal deductionAmount;
    private BigDecimal grossAmount;
    private BigDecimal netAmount;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
