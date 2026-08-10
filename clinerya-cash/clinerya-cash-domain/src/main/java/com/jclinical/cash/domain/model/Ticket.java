package com.jclinical.cash.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ticket {
    private UUID id;
    private UUID clinicId;
    private UUID cashSessionId;
    private UUID patientId;
    private UUID quotationId;
    private Integer folio;
    private BigDecimal totalAmount;
    private String concept;
    private UUID createdByStaffId;
    private LocalDateTime createdAt;
    private TicketStatus status;
    private UUID voidedByStaffId;
    private LocalDateTime voidedAt;
    private String voidReason;
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;
    private UUID discountAuthorizedByStaffId;
    private String discountReason;
    @Builder.Default
    private List<PaymentLine> paymentLines = new ArrayList<>();

    public BigDecimal sumPaymentLines() {
        return paymentLines.stream().map(PaymentLine::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal settledAmount() {
        BigDecimal discount = discountAmount != null ? discountAmount : BigDecimal.ZERO;
        return totalAmount.add(discount);
    }

    public BigDecimal cashPortion() {
        return paymentLines.stream()
                .filter(line -> line.getMethod() == PaymentMethod.CASH)
                .map(PaymentLine::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void voidTicket(UUID staffId, String reason) {
        if (status != TicketStatus.ACTIVE) {
            throw new IllegalStateException("Solo se puede anular un ticket activo.");
        }
        this.status = TicketStatus.VOIDED;
        this.voidedByStaffId = staffId;
        this.voidedAt = LocalDateTime.now();
        this.voidReason = reason;
    }
}
