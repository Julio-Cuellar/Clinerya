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
public class CashSession {
    private UUID id;
    private UUID clinicId;
    private UUID openedByStaffId;
    private LocalDateTime openedAt;
    private BigDecimal openingAmount;
    private UUID closedByStaffId;
    private LocalDateTime closedAt;
    private BigDecimal countedCashAmount;
    private BigDecimal expectedCashAmount;
    private BigDecimal cashDifference;
    private CashSessionStatus status;

    public void close(BigDecimal countedCashAmount, BigDecimal expectedCashAmount, UUID closedByStaffId) {
        if (status != CashSessionStatus.OPEN) {
            throw new IllegalStateException("Solo se puede cerrar un turno que está abierto.");
        }
        this.countedCashAmount = countedCashAmount;
        this.expectedCashAmount = expectedCashAmount;
        this.cashDifference = countedCashAmount.subtract(expectedCashAmount);
        this.closedByStaffId = closedByStaffId;
        this.closedAt = LocalDateTime.now();
        this.status = CashSessionStatus.CLOSED;
    }
}
