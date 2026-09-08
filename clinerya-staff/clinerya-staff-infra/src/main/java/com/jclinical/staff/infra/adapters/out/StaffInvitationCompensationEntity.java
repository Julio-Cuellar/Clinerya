package com.jclinical.staff.infra.adapters.out;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "staff_invitation_compensation", schema = "staff")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffInvitationCompensationEntity {
    @Id
    @Column(name = "invitation_id")
    private UUID invitationId;

    @Column(name = "clinic_id", nullable = false)
    private UUID clinicId;

    @Column(name = "base_salary", nullable = false)
    private BigDecimal baseSalary;

    @Column(name = "pay_frequency", nullable = false, length = 20)
    private String payFrequency;

    @Column(name = "payment_method", nullable = false, length = 20)
    private String paymentMethod;

    @Column(name = "payment_account_clabe", length = 18)
    private String paymentAccountClabe;

    @Column(length = 13)
    private String rfc;

    @Column(length = 18)
    private String curp;

    @Column(length = 11)
    private String nss;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
