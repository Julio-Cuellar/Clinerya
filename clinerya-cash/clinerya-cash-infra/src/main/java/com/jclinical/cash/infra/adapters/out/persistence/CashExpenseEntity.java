package com.jclinical.cash.infra.adapters.out.persistence;

import com.jclinical.cash.domain.model.CashExpenseStatus;
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
@Table(name = "cash_expenses", schema = "cash")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashExpenseEntity {

    @Id
    private UUID id;

    @Column(name = "clinic_id", nullable = false)
    private UUID clinicId;

    @Column(name = "cash_session_id", nullable = false)
    private UUID cashSessionId;

    @Column(nullable = false)
    private String concept;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "created_by_staff_id", nullable = false)
    private UUID createdByStaffId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CashExpenseStatus status;

    @Column(name = "voided_by_staff_id")
    private UUID voidedByStaffId;

    @Column(name = "voided_at")
    private LocalDateTime voidedAt;

    @Column(name = "void_reason")
    private String voidReason;
}
