package com.jclinical.cash.infra.adapters.out.persistence;

import com.jclinical.cash.domain.model.TicketStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
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

@Entity
@Table(name = "cash_tickets", schema = "cash")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketEntity {

    @Id
    private UUID id;

    @Column(name = "clinic_id", nullable = false)
    private UUID clinicId;

    @Column(name = "cash_session_id", nullable = false)
    private UUID cashSessionId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "quotation_id")
    private UUID quotationId;

    @Column(nullable = false)
    private Integer folio;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column
    private String concept;

    @Column(name = "created_by_staff_id", nullable = false)
    private UUID createdByStaffId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus status;

    @Column(name = "voided_by_staff_id")
    private UUID voidedByStaffId;

    @Column(name = "voided_at")
    private LocalDateTime voidedAt;

    @Column(name = "void_reason")
    private String voidReason;

    @Column(name = "discount_amount")
    private BigDecimal discountAmount;

    @Column(name = "discount_authorized_by_staff_id")
    private UUID discountAuthorizedByStaffId;

    @Column(name = "discount_reason")
    private String discountReason;

    @Builder.Default
    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<PaymentLineEntity> paymentLines = new ArrayList<>();
}
