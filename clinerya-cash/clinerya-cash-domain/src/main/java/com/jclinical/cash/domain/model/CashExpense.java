package com.jclinical.cash.domain.model;

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
public class CashExpense {
    private UUID id;
    private UUID clinicId;
    private UUID cashSessionId;
    private String concept;
    private BigDecimal amount;
    private UUID createdByStaffId;
    private LocalDateTime createdAt;
    private CashExpenseStatus status;
    private UUID voidedByStaffId;
    private LocalDateTime voidedAt;
    private String voidReason;

    public void voidExpense(UUID staffId, String reason) {
        if (status != CashExpenseStatus.ACTIVE) {
            throw new IllegalStateException("Solo se puede anular un egreso activo.");
        }
        this.status = CashExpenseStatus.VOIDED;
        this.voidedByStaffId = staffId;
        this.voidedAt = LocalDateTime.now();
        this.voidReason = reason;
    }
}
