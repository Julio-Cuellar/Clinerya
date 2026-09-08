package com.jclinical.staff.domain.service;

import com.jclinical.core.security.ClinicAccessDeniedException;
import com.jclinical.staff.domain.model.StaffCompensation;
import com.jclinical.staff.domain.model.StaffInvitationCompensation;
import com.jclinical.staff.domain.model.StaffPayFrequency;
import com.jclinical.staff.domain.model.StaffPaymentMethod;
import com.jclinical.staff.domain.model.StaffPermission;
import com.jclinical.staff.domain.ports.in.ManageStaffCompensationUseCase.CompensationInput;
import com.jclinical.staff.domain.ports.in.ManageStaffOnboardingUseCase;
import com.jclinical.staff.domain.ports.out.StaffCompensationRepositoryPort;
import com.jclinical.staff.domain.ports.out.StaffInvitationCompensationRepositoryPort;
import com.jclinical.staff.domain.ports.out.StaffPermissionCheckerPort;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class StaffOnboardingService implements ManageStaffOnboardingUseCase {

    private static final StaffPayFrequency DEFAULT_FREQUENCY = StaffPayFrequency.BIWEEKLY;
    private static final StaffPaymentMethod DEFAULT_METHOD = StaffPaymentMethod.BANK_TRANSFER;

    private final StaffPermissionCheckerPort permissionChecker;
    private final StaffInvitationCompensationRepositoryPort invitationCompensationRepository;
    private final StaffCompensationRepositoryPort compensationRepository;

    public StaffOnboardingService(StaffPermissionCheckerPort permissionChecker,
                                  StaffInvitationCompensationRepositoryPort invitationCompensationRepository,
                                  StaffCompensationRepositoryPort compensationRepository) {
        this.permissionChecker = permissionChecker;
        this.invitationCompensationRepository = invitationCompensationRepository;
        this.compensationRepository = compensationRepository;
    }

    @Override
    public void setInvitationCompensation(UUID clinicId, UUID actingUserId, UUID invitationId, CompensationInput compensation) {
        requirePayrollPermission(clinicId, actingUserId);
        if (invitationId == null) {
            throw new IllegalArgumentException("La invitacion es obligatoria.");
        }
        if (compensation == null) {
            invitationCompensationRepository.deleteByInvitationId(invitationId);
            return;
        }
        invitationCompensationRepository.save(StaffInvitationCompensation.builder()
                .invitationId(invitationId)
                .clinicId(clinicId)
                .baseSalary(amountOrZero(compensation.baseSalary()))
                .payFrequency(compensation.payFrequency() != null ? compensation.payFrequency() : DEFAULT_FREQUENCY)
                .paymentMethod(compensation.paymentMethod() != null ? compensation.paymentMethod() : DEFAULT_METHOD)
                .paymentAccountClabe(blankToNull(compensation.paymentAccountClabe()))
                .rfc(blankToNull(compensation.rfc()))
                .curp(blankToNull(compensation.curp()))
                .nss(blankToNull(compensation.nss()))
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Override
    public void applyInvitationCompensation(UUID invitationId, UUID staffId, UUID clinicId) {
        if (invitationId == null || staffId == null) {
            return;
        }
        invitationCompensationRepository.findByInvitationId(invitationId).ifPresent(pending -> {
            compensationRepository.save(StaffCompensation.builder()
                    .staffId(staffId)
                    .clinicId(clinicId != null ? clinicId : pending.getClinicId())
                    .baseSalary(amountOrZero(pending.getBaseSalary()))
                    .payFrequency(pending.getPayFrequency() != null ? pending.getPayFrequency() : DEFAULT_FREQUENCY)
                    .paymentMethod(pending.getPaymentMethod() != null ? pending.getPaymentMethod() : DEFAULT_METHOD)
                    .paymentAccountClabe(pending.getPaymentAccountClabe())
                    .rfc(pending.getRfc())
                    .curp(pending.getCurp())
                    .nss(pending.getNss())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build());
            invitationCompensationRepository.deleteByInvitationId(invitationId);
        });
    }

    private void requirePayrollPermission(UUID clinicId, UUID actingUserId) {
        if (actingUserId == null) {
            throw new ClinicAccessDeniedException("Usuario no autenticado.");
        }
        if (!permissionChecker.hasPermission(clinicId, actingUserId, StaffPermission.MANAGE_PAYROLL)) {
            throw new ClinicAccessDeniedException("No tienes permiso para gestionar la nomina de esta clinica.");
        }
    }

    private BigDecimal amountOrZero(BigDecimal amount) {
        return amount != null && amount.signum() >= 0 ? amount : BigDecimal.ZERO;
    }

    private String blankToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
