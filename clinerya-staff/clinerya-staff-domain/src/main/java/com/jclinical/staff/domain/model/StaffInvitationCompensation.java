package com.jclinical.staff.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Compensacion que el admin captura al invitar a un empleado. Vive hasta que el
 * invitado confirma su registro y se copia a {@link StaffCompensation}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffInvitationCompensation {
    private UUID invitationId;
    private UUID clinicId;
    private BigDecimal baseSalary;
    private StaffPayFrequency payFrequency;
    private StaffPaymentMethod paymentMethod;
    private String paymentAccountClabe;
    private String rfc;
    private String curp;
    private String nss;
    private LocalDateTime createdAt;
}
