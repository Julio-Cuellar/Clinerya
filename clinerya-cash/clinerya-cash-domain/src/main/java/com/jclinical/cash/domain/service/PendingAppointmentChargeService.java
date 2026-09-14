package com.jclinical.cash.domain.service;

import com.jclinical.cash.domain.ports.in.ManagePendingAppointmentChargesUseCase;
import com.jclinical.cash.domain.ports.out.CashAppointmentPort;
import com.jclinical.cash.domain.ports.out.CashAppointmentPort.CompletedAppointmentSnapshot;
import com.jclinical.cash.domain.ports.out.CashQuotationValidatorPort;
import com.jclinical.cash.domain.ports.out.CashQuotationValidatorPort.QuotationItemSnapshot;
import com.jclinical.cash.domain.ports.out.CashQuotationValidatorPort.QuotationSnapshot;
import com.jclinical.cash.domain.ports.out.TicketRepositoryPort;
import com.jclinical.core.security.ClinicAccessDeniedException;
import com.jclinical.core.security.StaffPermission;
import com.jclinical.core.security.StaffPermissionCheckerPort;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PendingAppointmentChargeService implements ManagePendingAppointmentChargesUseCase {

    private final CashAppointmentPort appointmentPort;
    private final CashQuotationValidatorPort quotationValidator;
    private final TicketRepositoryPort ticketRepository;
    private final StaffPermissionCheckerPort permissionChecker;

    public PendingAppointmentChargeService(
            CashAppointmentPort appointmentPort,
            CashQuotationValidatorPort quotationValidator,
            TicketRepositoryPort ticketRepository,
            StaffPermissionCheckerPort permissionChecker) {
        this.appointmentPort = appointmentPort;
        this.quotationValidator = quotationValidator;
        this.ticketRepository = ticketRepository;
        this.permissionChecker = permissionChecker;
    }

    @Override
    public List<PendingAppointmentCharge> listPendingCharges(UUID actingUserId, UUID clinicId) {
        if (clinicId == null) {
            throw new IllegalArgumentException("La clinica es obligatoria.");
        }
        if (actingUserId == null) {
            throw new ClinicAccessDeniedException("Usuario no autenticado.");
        }
        if (!permissionChecker.hasPermission(clinicId, actingUserId, StaffPermission.VIEW_CASH)) {
            throw new ClinicAccessDeniedException("No tienes permiso para consultar la caja de esta clinica.");
        }

        Map<UUID, List<CompletedAppointmentSnapshot>> appointmentsByQuotation = new LinkedHashMap<>();
        appointmentPort.listCompletedAppointments(clinicId).stream()
                .filter(this::hasBillableReference)
                .sorted(Comparator.comparing(this::completedAtOrMinimum))
                .forEach(appointment -> appointmentsByQuotation
                        .computeIfAbsent(appointment.quotationId(), ignored -> new ArrayList<>())
                        .add(appointment));

        List<PendingAppointmentCharge> pendingCharges = new ArrayList<>();
        appointmentsByQuotation.forEach((quotationId, appointments) ->
                appendQuotationCharges(clinicId, quotationId, appointments, pendingCharges));
        pendingCharges.sort(Comparator.comparing(PendingAppointmentCharge::completedAt).reversed());
        return pendingCharges;
    }

    private void appendQuotationCharges(
            UUID clinicId,
            UUID quotationId,
            List<CompletedAppointmentSnapshot> appointments,
            List<PendingAppointmentCharge> destination) {
        CompletedAppointmentSnapshot first = appointments.get(0);
        QuotationSnapshot quotation = quotationValidator
                .findQuotation(quotationId, first.patientId(), clinicId)
                .filter(QuotationSnapshot::accepted)
                .orElse(null);
        if (quotation == null) {
            return;
        }

        BigDecimal paidAmount = safe(ticketRepository.sumActiveAmountByQuotation(quotationId, clinicId));
        BigDecimal remainingBalance = safe(quotation.grandTotal()).subtract(paidAmount).max(BigDecimal.ZERO);
        if (remainingBalance.signum() == 0) {
            return;
        }

        for (CompletedAppointmentSnapshot appointment : appointments) {
            if (remainingBalance.signum() == 0) {
                break;
            }
            for (UUID quotationItemId : appointment.effectiveQuotationItemIds()) {
                if (remainingBalance.signum() == 0) {
                    break;
                }
                QuotationItemSnapshot item = findItem(quotation, quotationItemId);
                if (item == null || safe(item.subtotal()).signum() <= 0) {
                    continue;
                }

                BigDecimal itemSubtotal = safe(item.subtotal());
                if (paidAmount.compareTo(itemSubtotal) >= 0) {
                    paidAmount = paidAmount.subtract(itemSubtotal);
                    continue;
                }

                BigDecimal unpaidForItem = itemSubtotal.subtract(paidAmount);
                paidAmount = BigDecimal.ZERO;

                BigDecimal pendingAmount = unpaidForItem.min(remainingBalance);
                if (pendingAmount.signum() > 0) {
                    destination.add(new PendingAppointmentCharge(
                            appointment.appointmentId(),
                            appointment.patientId(),
                            appointment.doctorStaffId(),
                            quotationId,
                            quotationItemId,
                            buildConcept(appointment, item),
                            pendingAmount,
                            completedAtOrMinimum(appointment)
                    ));
                    remainingBalance = remainingBalance.subtract(pendingAmount);
                }
            }
        }
    }

    private boolean hasBillableReference(CompletedAppointmentSnapshot appointment) {
        return appointment != null
                && appointment.appointmentId() != null
                && appointment.patientId() != null
                && appointment.quotationId() != null
                && !appointment.effectiveQuotationItemIds().isEmpty();
    }

    private QuotationItemSnapshot findItem(QuotationSnapshot quotation, UUID quotationItemId) {
        if (quotation.items() == null) {
            return null;
        }
        return quotation.items().stream()
                .filter(item -> quotationItemId.equals(item.quotationItemId()))
                .findFirst()
                .orElse(null);
    }

    private String buildConcept(CompletedAppointmentSnapshot appointment, QuotationItemSnapshot item) {
        String reason = clean(appointment.reason());
        String description = clean(item.description());
        return "Cita: " + (reason != null ? reason : description != null ? description : "Procedimiento odontologico");
    }

    private LocalDateTime completedAtOrMinimum(CompletedAppointmentSnapshot appointment) {
        return appointment.completedAt() != null ? appointment.completedAt() : LocalDateTime.MIN;
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
