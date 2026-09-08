package com.jclinical.staff.domain.service;

import com.jclinical.core.security.ClinicAccessDeniedException;
import com.jclinical.staff.domain.model.ClinicStaff;
import com.jclinical.staff.domain.model.StaffCompensation;
import com.jclinical.staff.domain.model.StaffPayFrequency;
import com.jclinical.staff.domain.model.StaffPaymentMethod;
import com.jclinical.staff.domain.model.StaffPermission;
import com.jclinical.staff.domain.ports.in.ManageStaffCompensationUseCase;
import com.jclinical.staff.domain.ports.out.ClinicStaffRepositoryPort;
import com.jclinical.staff.domain.ports.out.StaffCompensationRepositoryPort;
import com.jclinical.staff.domain.ports.out.StaffPermissionCheckerPort;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class StaffCompensationService implements ManageStaffCompensationUseCase {

    private static final StaffPayFrequency DEFAULT_FREQUENCY = StaffPayFrequency.BIWEEKLY;
    private static final StaffPaymentMethod DEFAULT_METHOD = StaffPaymentMethod.BANK_TRANSFER;

    private final StaffCompensationRepositoryPort compensationRepository;
    private final ClinicStaffRepositoryPort clinicStaffRepository;
    private final StaffPermissionCheckerPort permissionChecker;

    public StaffCompensationService(StaffCompensationRepositoryPort compensationRepository,
                                    ClinicStaffRepositoryPort clinicStaffRepository,
                                    StaffPermissionCheckerPort permissionChecker) {
        this.compensationRepository = compensationRepository;
        this.clinicStaffRepository = clinicStaffRepository;
        this.permissionChecker = permissionChecker;
    }

    @Override
    public CompensationSummary getCompensation(UUID clinicId, UUID staffId) {
        ensureStaffInClinic(clinicId, staffId);
        return compensationRepository.findByStaffId(staffId)
                .map(this::toSummary)
                .orElseGet(() -> emptySummary(clinicId, staffId));
    }

    @Override
    public List<CompensationSummary> listByClinic(UUID clinicId) {
        return compensationRepository.findByClinicId(clinicId).stream()
                .map(this::toSummary)
                .toList();
    }

    @Override
    public CompensationSummary updateCompensation(UUID clinicId, UUID actingUserId, UUID staffId, CompensationInput input) {
        requirePayrollPermission(clinicId, actingUserId);
        ensureStaffInClinic(clinicId, staffId);
        if (input == null) {
            throw new IllegalArgumentException("Los datos de compensacion son obligatorios.");
        }
        BigDecimal baseSalary = input.baseSalary() != null ? input.baseSalary() : BigDecimal.ZERO;
        if (baseSalary.signum() < 0) {
            throw new IllegalArgumentException("El sueldo base no puede ser negativo.");
        }

        StaffCompensation compensation = compensationRepository.findByStaffId(staffId)
                .orElseGet(() -> StaffCompensation.builder()
                        .staffId(staffId)
                        .clinicId(clinicId)
                        .createdAt(LocalDateTime.now())
                        .build());
        compensation.setClinicId(clinicId);
        compensation.setBaseSalary(baseSalary);
        compensation.setPayFrequency(input.payFrequency() != null ? input.payFrequency() : DEFAULT_FREQUENCY);
        compensation.setPaymentMethod(input.paymentMethod() != null ? input.paymentMethod() : DEFAULT_METHOD);
        compensation.setPaymentAccountClabe(blankToNull(input.paymentAccountClabe()));
        compensation.setRfc(blankToNull(input.rfc()));
        compensation.setCurp(blankToNull(input.curp()));
        compensation.setNss(blankToNull(input.nss()));
        compensation.setUpdatedAt(LocalDateTime.now());
        return toSummary(compensationRepository.save(compensation));
    }

    private void requirePayrollPermission(UUID clinicId, UUID actingUserId) {
        if (actingUserId == null) {
            throw new ClinicAccessDeniedException("Usuario no autenticado.");
        }
        if (!permissionChecker.hasPermission(clinicId, actingUserId, StaffPermission.MANAGE_PAYROLL)) {
            throw new ClinicAccessDeniedException("No tienes permiso para gestionar la nomina de esta clinica.");
        }
    }

    private void ensureStaffInClinic(UUID clinicId, UUID staffId) {
        ClinicStaff staff = clinicStaffRepository.findById(staffId)
                .orElseThrow(() -> new IllegalArgumentException("Miembro del personal no encontrado."));
        if (!staff.getClinicId().equals(clinicId) || !staff.isActive()) {
            throw new IllegalArgumentException("El miembro del personal no esta activo en esta clinica.");
        }
    }

    private CompensationSummary emptySummary(UUID clinicId, UUID staffId) {
        return new CompensationSummary(staffId, clinicId, BigDecimal.ZERO, DEFAULT_FREQUENCY, DEFAULT_METHOD,
                null, null, null, null);
    }

    private CompensationSummary toSummary(StaffCompensation compensation) {
        return new CompensationSummary(
                compensation.getStaffId(),
                compensation.getClinicId(),
                compensation.getBaseSalary() != null ? compensation.getBaseSalary() : BigDecimal.ZERO,
                compensation.getPayFrequency() != null ? compensation.getPayFrequency() : DEFAULT_FREQUENCY,
                compensation.getPaymentMethod() != null ? compensation.getPaymentMethod() : DEFAULT_METHOD,
                compensation.getPaymentAccountClabe(),
                compensation.getRfc(),
                compensation.getCurp(),
                compensation.getNss());
    }

    private String blankToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
