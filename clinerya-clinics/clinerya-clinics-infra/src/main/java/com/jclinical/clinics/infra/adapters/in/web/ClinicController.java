package com.jclinical.clinics.infra.adapters.in.web;

import com.jclinical.clinics.domain.model.Clinic;
import com.jclinical.clinics.domain.ports.in.ManageClinicUseCase;
import com.jclinical.clinics.infra.adapters.in.web.dto.ClinicResponse;
import com.jclinical.clinics.infra.adapters.in.web.dto.CreateClinicRequest;
import com.jclinical.clinics.infra.adapters.in.web.dto.UpdateClinicRequest;
import com.jclinical.clinics.infra.adapters.out.ClinicMapper;
import com.jclinical.clinics.infra.adapters.out.accounting.DefaultPettyCashProvisioner;
import com.jclinical.users.domain.model.User;
import com.jclinical.users.domain.ports.out.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clinics")
@RequiredArgsConstructor
public class ClinicController {

    private final ManageClinicUseCase manageClinicUseCase;
    private final UserRepositoryPort userRepository;
    private final ClinicMapper clinicMapper;
    private final DefaultPettyCashProvisioner pettyCashProvisioner;

    @PostMapping
    public ResponseEntity<ClinicResponse> createClinic(@RequestBody CreateClinicRequest request) {
        UUID ownerUserId = getAuthenticatedUserId();
        
        Clinic clinic = manageClinicUseCase.createClinic(
                ownerUserId,
                request.name(),
                request.email(),
                request.timezone(),
                request.legalName(),
                request.rfc(),
                request.taxRegimeCode(),
                request.addressStreet(),
                request.addressColonia(),
                request.addressMunicipality(),
                request.addressState(),
                request.addressZip(),
                request.phone(),
                request.cofeprisPermitNumber(),
                request.responsibleDoctorName(),
                request.responsibleDoctorProfessionalLicense()
        );

        pettyCashProvisioner.ensureForClinic(clinic.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(clinicMapper.toResponse(clinic));
    }

    @PutMapping("/{clinicId}")
    public ResponseEntity<ClinicResponse> updateClinic(
            @PathVariable UUID clinicId,
            @RequestBody UpdateClinicRequest request) {
        UUID ownerUserId = getAuthenticatedUserId();

        Clinic clinic = manageClinicUseCase.updateClinic(
                ownerUserId,
                clinicId,
                request.name(),
                request.legalName(),
                request.rfc(),
                request.taxRegimeCode(),
                request.addressStreet(),
                request.addressColonia(),
                request.addressMunicipality(),
                request.addressState(),
                request.addressZip(),
                request.phone(),
                request.email(),
                request.logoUrl(),
                request.timezone(),
                request.privacyNoticeUrl(),
                request.cofeprisPermitNumber(),
                request.responsibleDoctorName(),
                request.responsibleDoctorProfessionalLicense(),
                request.materialReservationLeadDays()
        );

        return ResponseEntity.ok(clinicMapper.toResponse(clinic));
    }

    @GetMapping
    public ResponseEntity<List<ClinicResponse>> getClinics() {
        UUID ownerUserId = getAuthenticatedUserId();
        List<Clinic> clinics = manageClinicUseCase.getClinicsByOwner(ownerUserId);
        List<ClinicResponse> response = clinics.stream()
                .map(clinicMapper::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{clinicId}")
    public ResponseEntity<ClinicResponse> getClinic(@PathVariable UUID clinicId) {
        UUID ownerUserId = getAuthenticatedUserId();
        Clinic clinic = manageClinicUseCase.getClinic(ownerUserId, clinicId);
        return ResponseEntity.ok(clinicMapper.toResponse(clinic));
    }

    private UUID getAuthenticatedUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Usuario no autenticado");
        }
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        return user.getId();
    }
}
