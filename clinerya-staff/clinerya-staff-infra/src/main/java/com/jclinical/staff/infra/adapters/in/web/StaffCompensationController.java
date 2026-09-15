package com.jclinical.staff.infra.adapters.in.web;

import com.jclinical.staff.domain.model.StaffPayFrequency;
import com.jclinical.staff.domain.model.StaffPaymentMethod;
import com.jclinical.staff.domain.ports.in.ManageStaffCompensationUseCase;
import com.jclinical.staff.domain.ports.in.ManageStaffCompensationUseCase.CompensationInput;
import com.jclinical.staff.domain.ports.in.ManageStaffCompensationUseCase.CompensationSummary;
import com.jclinical.staff.domain.ports.out.UserDirectoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clinics/{clinicId}/staff")
@RequiredArgsConstructor
public class StaffCompensationController {

    private final ManageStaffCompensationUseCase compensationUseCase;
    private final UserDirectoryPort userDirectory;

    @GetMapping("/compensation")
    public ResponseEntity<List<CompensationSummary>> listCompensation(@PathVariable UUID clinicId) {
        return ResponseEntity.ok(compensationUseCase.listByClinic(clinicId));
    }

    @GetMapping("/{staffId}/compensation")
    public ResponseEntity<CompensationSummary> getCompensation(
            @PathVariable UUID clinicId,
            @PathVariable UUID staffId) {
        return ResponseEntity.ok(compensationUseCase.getCompensation(clinicId, staffId));
    }

    @PutMapping("/{staffId}/compensation")
    public ResponseEntity<CompensationSummary> updateCompensation(
            @PathVariable UUID clinicId,
            @PathVariable UUID staffId,
            @RequestBody CompensationRequest request,
            Principal principal) {
        CompensationInput input = new CompensationInput(
                request.baseSalary(),
                parseFrequency(request.payFrequency()),
                parseMethod(request.paymentMethod()),
                request.paymentAccountClabe(),
                request.rfc(),
                request.curp(),
                request.nss());
        return ResponseEntity.ok(compensationUseCase.updateCompensation(clinicId, currentUserId(principal), staffId, input));
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

    private UUID currentUserId(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new IllegalStateException("Usuario no autenticado.");
        }
        return userDirectory.findByEmail(principal.getName())
                .map(UserDirectoryPort.UserSummary::id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
    }

    public record CompensationRequest(
            BigDecimal baseSalary,
            String payFrequency,
            String paymentMethod,
            String paymentAccountClabe,
            String rfc,
            String curp,
            String nss
    ) {}
}
