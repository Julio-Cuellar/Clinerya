package com.jclinical.staff.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Datos de compensacion base de un empleado (uno por miembro del personal). */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffCompensation {
    private UUID staffId;
    private UUID clinicId;
    private BigDecimal baseSalary;
    private StaffPayFrequency payFrequency;
    private StaffPaymentMethod paymentMethod;
    private String paymentAccountClabe;
    private String rfc;
    private String curp;
    private String nss;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
