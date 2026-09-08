package com.jclinical.staff.domain.ports.in;

import com.jclinical.staff.domain.model.StaffPayFrequency;
import com.jclinical.staff.domain.model.StaffPaymentMethod;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ManageStaffCompensationUseCase {

    CompensationSummary getCompensation(UUID clinicId, UUID staffId);

    List<CompensationSummary> listByClinic(UUID clinicId);

    CompensationSummary updateCompensation(UUID clinicId, UUID actingUserId, UUID staffId, CompensationInput input);

    record CompensationInput(
            BigDecimal baseSalary,
            StaffPayFrequency payFrequency,
            StaffPaymentMethod paymentMethod,
            String paymentAccountClabe,
            String rfc,
            String curp,
            String nss
    ) {}

    record CompensationSummary(
            UUID staffId,
            UUID clinicId,
            BigDecimal baseSalary,
            StaffPayFrequency payFrequency,
            StaffPaymentMethod paymentMethod,
            String paymentAccountClabe,
            String rfc,
            String curp,
            String nss
    ) {}
}
