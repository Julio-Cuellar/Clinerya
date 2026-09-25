package com.jclinical.clinics.infra.adapters.in.web;

import com.jclinical.clinics.domain.model.Clinic;
import com.jclinical.clinics.domain.model.ResponsibleDoctorSetup;
import com.jclinical.clinics.domain.ports.in.ManageClinicUseCase;
import com.jclinical.clinics.infra.adapters.in.web.dto.ClinicResponse;
import com.jclinical.clinics.infra.adapters.in.web.dto.CompleteClinicSetupRequest;
import com.jclinical.clinics.infra.adapters.in.web.dto.CreateClinicRequest;
import com.jclinical.clinics.infra.adapters.in.web.dto.UpdateClinicRequest;
import com.jclinical.clinics.infra.adapters.in.web.dto.UpdateClinicSpecialtyRequest;
import com.jclinical.clinics.infra.adapters.out.ClinicMapper;
import com.jclinical.clinics.infra.adapters.out.accounting.DefaultPettyCashProvisioner;
import com.jclinical.users.infra.security.CurrentUserResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clinics")
@RequiredArgsConstructor
public class ClinicController {

    private final ManageClinicUseCase manageClinicUseCase;
    private final CurrentUserResolver currentUserResolver;
    private final ClinicMapper clinicMapper;
    private final DefaultPettyCashProvisioner pettyCashProvisioner;

    @PostMapping
    @Transactional
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
                new ResponsibleDoctorSetup(
                        Boolean.TRUE.equals(request.ownerIsResponsibleDoctor()),
                        request.responsibleDoctorName(),
                        request.responsibleDoctorProfessionalLicense(),
                        request.ownerAttendsPatients(),
                        request.ownerCedulaProfesional())
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

    // Pantalla "Completa los datos de tu clinica": datos, medico responsable y papel del titular
    // en un solo paso, para no dejar la clinica guardada sin decidir quien atiende.
    @PutMapping("/{clinicId}/setup")
    @Transactional
    public ResponseEntity<ClinicResponse> completeSetup(
            @PathVariable UUID clinicId,
            @RequestBody CompleteClinicSetupRequest request) {
        UUID ownerUserId = getAuthenticatedUserId();

        Clinic clinic = manageClinicUseCase.completeSetup(
                ownerUserId,
                clinicId,
                new ManageClinicUseCase.ClinicSetupDetails(
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
                        request.logoUrl(),
                        request.cofeprisPermitNumber()),
                new ResponsibleDoctorSetup(
                        Boolean.TRUE.equals(request.ownerIsResponsibleDoctor()),
                        request.responsibleDoctorName(),
                        request.responsibleDoctorProfessionalLicense(),
                        request.ownerAttendsPatients(),
                        request.ownerCedulaProfesional()));

        return ResponseEntity.ok(clinicMapper.toResponse(clinic));
    }

    @PutMapping("/{clinicId}/specialty")
    public ResponseEntity<ClinicResponse> updateSpecialty(
            @PathVariable UUID clinicId,
            @RequestBody UpdateClinicSpecialtyRequest request) {
        UUID actingUserId = getAuthenticatedUserId();
        Clinic clinic = manageClinicUseCase.updateSpecialty(actingUserId, clinicId, request.specialty());
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
        return currentUserResolver.getCurrentUserId();
    }
}
