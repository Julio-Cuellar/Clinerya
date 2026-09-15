package com.jclinical.staff.infra.adapters.in.web;

import com.jclinical.staff.domain.model.StaffPayFrequency;
import com.jclinical.staff.domain.model.StaffPaymentMethod;
import com.jclinical.staff.domain.ports.in.ManageStaffCompensationUseCase.CompensationInput;
import com.jclinical.staff.domain.ports.in.ManageStaffOnboardingUseCase;
import com.jclinical.staff.domain.ports.out.UserDirectoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.UUID;

/**
 * Adjunta los datos de nomina a una invitacion de personal ya creada (via el
 * endpoint estandar de invitaciones). Se aplican al confirmar el registro.
 */
@RestController
@RequestMapping("/api/v1/clinics/{clinicId}/staff/onboarding")
@RequiredArgsConstructor
public class StaffOnboardingController {

    private final ManageStaffOnboardingUseCase onboardingUseCase;
    private final UserDirectoryPort userDirectory;

    @PutMapping("/invitations/{invitationId}/compensation")
    public ResponseEntity<Void> setInvitationCompensation(
            @PathVariable UUID clinicId,
            @PathVariable UUID invitationId,
            @RequestBody CompensationPayload payload,
            Principal principal) {
        onboardingUseCase.setInvitationCompensation(
                clinicId,
                currentUserId(principal),
                invitationId,
                toCompensationInput(payload));
        return ResponseEntity.noContent().build();
    }

    private UUID currentUserId(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new IllegalStateException("Usuario no autenticado.");
        }
        return userDirectory.findByEmail(principal.getName())
                .map(UserDirectoryPort.UserSummary::id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
    }

    private CompensationInput toCompensationInput(CompensationPayload payload) {
        if (payload == null) {
            return null;
        }
        return new CompensationInput(
                payload.baseSalary(),
                parseFrequency(payload.payFrequency()),
                parseMethod(payload.paymentMethod()),
                payload.paymentAccountClabe(),
                payload.rfc(),
                payload.curp(),
                payload.nss());
    }

    private StaffPayFrequency parseFrequency(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return StaffPayFrequency.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Periodicidad de pago no valida: " + value);
        }
    }

    private StaffPaymentMethod parseMethod(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return StaffPaymentMethod.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Metodo de pago no valido: " + value);
        }
    }

    public record CompensationPayload(
            BigDecimal baseSalary,
            String payFrequency,
            String paymentMethod,
            String paymentAccountClabe,
            String rfc,
            String curp,
            String nss
    ) {}
}
