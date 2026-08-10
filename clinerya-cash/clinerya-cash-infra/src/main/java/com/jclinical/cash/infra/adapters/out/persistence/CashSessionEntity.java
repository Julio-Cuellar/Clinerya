package com.jclinical.cash.infra.adapters.out.persistence;

import com.jclinical.cash.domain.model.CashSessionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cash_sessions", schema = "cash")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashSessionEntity {

    @Id
    private UUID id;

    @Column(name = "clinic_id", nullable = false)
    private UUID clinicId;

    @Column(name = "opened_by_staff_id", nullable = false)
    private UUID openedByStaffId;

    @Column(name = "opened_at", nullable = false)
    private LocalDateTime openedAt;

    @Column(name = "opening_amount", nullable = false)
    private BigDecimal openingAmount;

    @Column(name = "closed_by_staff_id")
    private UUID closedByStaffId;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "counted_cash_amount")
    private BigDecimal countedCashAmount;

    @Column(name = "expected_cash_amount")
    private BigDecimal expectedCashAmount;

    @Column(name = "cash_difference")
    private BigDecimal cashDifference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CashSessionStatus status;
}
