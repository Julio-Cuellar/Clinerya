package com.jclinical.clinics.domain.service;

import com.jclinical.clinics.domain.model.*;
import com.jclinical.clinics.domain.ports.in.GetClinicPublicProfileUseCase;
import com.jclinical.clinics.domain.ports.in.GetClinicSettingsUseCase;
import com.jclinical.clinics.domain.ports.in.ManageClinicUseCase;
import com.jclinical.clinics.domain.ports.out.ClinicRepositoryPort;
import com.jclinical.core.domain.ClinicSpecialty;
import com.jclinical.core.security.ClinicAccessDeniedException;
import com.jclinical.core.security.StaffPermission;
import com.jclinical.core.security.StaffPermissionCheckerPort;
import com.jclinical.staff.domain.model.ClinicStaff;
import com.jclinical.staff.domain.ports.in.ManageClinicStaffUseCase;
import com.jclinical.staff.domain.ports.out.ClinicStaffRepositoryPort;
import com.jclinical.staff.domain.ports.out.DoctorProfileRepositoryPort;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ManageClinicService implements ManageClinicUseCase, GetClinicSettingsUseCase, GetClinicPublicProfileUseCase {

    private final ClinicRepositoryPort clinicRepository;
    private final ClinicStaffRepositoryPort clinicStaffRepository;
    private final DoctorProfileRepositoryPort doctorProfileRepository;
    private final StaffPermissionCheckerPort permissionChecker;
    private final ManageClinicStaffUseCase clinicStaffUseCase;

    public ManageClinicService(ClinicRepositoryPort clinicRepository,
                               ClinicStaffRepositoryPort clinicStaffRepository,
                               DoctorProfileRepositoryPort doctorProfileRepository,
                               StaffPermissionCheckerPort permissionChecker,
                               ManageClinicStaffUseCase clinicStaffUseCase) {
        this.clinicRepository = clinicRepository;
        this.clinicStaffRepository = clinicStaffRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.permissionChecker = permissionChecker;
        this.clinicStaffUseCase = clinicStaffUseCase;
    }

    @Override
    public Clinic createClinic(
            UUID ownerUserId,
            String name,
            String email,
            String timezone,
            String legalName,
            String rfc,
            String taxRegimeCode,
            String addressStreet,
            String addressColonia,
            String addressMunicipality,
            String addressState,
            String addressZip,
            String phone,
            String cofeprisPermitNumber,
            ResponsibleDoctorSetup responsibleDoctor) {

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de la clínica es obligatorio");
        }
        ResponsibleDoctorSetup.Resolved responsible = responsibleDoctor.resolve();

        // 1. Crear Clínica activa
        Clinic clinic = Clinic.builder()
                .id(UUID.randomUUID())
                .ownerUserId(ownerUserId)
                .name(name.trim())
                .email(email != null ? email.trim() : null)
                .timezone(timezone != null ? timezone.trim() : "America/Mexico_City")
                .specialty(ClinicSpecialty.SIN_CONFIGURAR)
                .legalName(legalName != null ? legalName.trim() : null)
                .rfc(rfc != null ? rfc.trim() : null)
                .taxRegimeCode(taxRegimeCode != null ? taxRegimeCode.trim() : null)
                .addressStreet(addressStreet != null ? addressStreet.trim() : null)
                .addressColonia(addressColonia != null ? addressColonia.trim() : null)
                .addressMunicipality(addressMunicipality != null ? addressMunicipality.trim() : null)
                .addressState(addressState != null ? addressState.trim() : null)
                .addressZip(addressZip != null ? addressZip.trim() : null)
                .phone(phone != null ? phone.trim() : null)
                .cofeprisPermitNumber(cofeprisPermitNumber != null ? cofeprisPermitNumber.trim() : null)
                .responsibleDoctorName(responsible.responsibleDoctorName())
                .responsibleDoctorProfessionalLicense(responsible.responsibleDoctorProfessionalLicense())
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // 2. El propietario administra la clinica; si ademas atiende pacientes, con su cedula.
        ClinicOwnerStaff owner = ClinicOwnerStaff.create(clinic.getId(), ownerUserId, true,
                responsible.ownerAttendsPatients(), responsible.ownerCedulaProfesional());

        // 3. Asignar representante legal
        clinic.assignLegalRepresentative(owner.staff().getId());

        // 4. Guardar todo
        clinicRepository.save(clinic);
        clinicStaffRepository.save(owner.staff());
        owner.doctorProfile().ifPresent(doctorProfileRepository::save);

        return clinic;
    }

    @Override
    public Clinic completeSetup(UUID ownerUserId, UUID clinicId, ClinicSetupDetails details,
                                ResponsibleDoctorSetup responsibleDoctor) {
        // Se valida todo antes de escribir: una cedula mal capturada no debe dejar guardada la
        // clinica a medias.
        ResponsibleDoctorSetup.Resolved responsible = responsibleDoctor.resolve();
        if (details.name() == null || details.name().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de la clínica es obligatorio");
        }
        Clinic clinic = updateClinic(
                ownerUserId,
                clinicId,
                details.name(),
                details.legalName(),
                details.rfc(),
                details.taxRegimeCode(),
                details.addressStreet(),
                details.addressColonia(),
                details.addressMunicipality(),
                details.addressState(),
                details.addressZip(),
                details.phone(),
                details.email(),
                details.logoUrl(),
                details.timezone(),
                null,
                details.cofeprisPermitNumber(),
                responsible.responsibleDoctorName(),
                responsible.responsibleDoctorProfessionalLicense(),
                null);

        ClinicStaff owner = clinicStaffRepository.findByClinicIdAndUserId(clinicId, ownerUserId)
                .orElseThrow(() -> new IllegalStateException("El titular no está registrado como personal de la clínica."));
        clinicStaffUseCase.updateClinicalPractice(clinicId, owner.getId(),
                responsible.ownerAttendsPatients(), responsible.ownerCedulaProfesional());
        return clinic;
    }

    @Override
    public Clinic updateClinic(
            UUID ownerUserId,
            UUID clinicId,
            String name,
            String legalName,
            String rfc,
            String taxRegimeCode,
            String addressStreet,
            String addressColonia,
            String addressMunicipality,
            String addressState,
            String addressZip,
            String phone,
            String email,
            String logoUrl,
            String timezone,
            String privacyNoticeUrl,
            String cofeprisPermitNumber,
            String responsibleDoctorName,
            String responsibleDoctorProfessionalLicense,
            Integer materialReservationLeadDays) {

        Clinic clinic = clinicRepository.findById(clinicId)
                .orElseThrow(() -> new IllegalArgumentException("Clínica no encontrada"));

        if (!clinic.getOwnerUserId().equals(ownerUserId)) {
            throw new IllegalStateException("No tienes permisos para modificar esta clínica");
        }

        if (name != null && !name.trim().isEmpty()) {
            clinic.setName(name.trim());
        }
        clinic.setLegalName(legalName != null ? legalName.trim() : clinic.getLegalName());
        clinic.setRfc(rfc != null ? rfc.trim() : clinic.getRfc());
        clinic.setTaxRegimeCode(taxRegimeCode != null ? taxRegimeCode.trim() : clinic.getTaxRegimeCode());
        clinic.setAddressStreet(addressStreet != null ? addressStreet.trim() : clinic.getAddressStreet());
        clinic.setAddressColonia(addressColonia != null ? addressColonia.trim() : clinic.getAddressColonia());
        clinic.setAddressMunicipality(addressMunicipality != null ? addressMunicipality.trim() : clinic.getAddressMunicipality());
        clinic.setAddressState(addressState != null ? addressState.trim() : clinic.getAddressState());
        clinic.setAddressZip(addressZip != null ? addressZip.trim() : clinic.getAddressZip());
        clinic.setPhone(phone != null ? phone.trim() : clinic.getPhone());
        clinic.setEmail(email != null ? email.trim() : clinic.getEmail());
        clinic.setLogoUrl(logoUrl != null ? logoUrl.trim() : clinic.getLogoUrl());
        clinic.setTimezone(timezone != null ? timezone.trim() : clinic.getTimezone());
        clinic.setPrivacyNoticeUrl(privacyNoticeUrl != null ? privacyNoticeUrl.trim() : clinic.getPrivacyNoticeUrl());
        clinic.setCofeprisPermitNumber(cofeprisPermitNumber != null ? cofeprisPermitNumber.trim() : clinic.getCofeprisPermitNumber());
        clinic.setResponsibleDoctorName(responsibleDoctorName != null ? responsibleDoctorName.trim() : clinic.getResponsibleDoctorName());
        clinic.setResponsibleDoctorProfessionalLicense(responsibleDoctorProfessionalLicense != null ? responsibleDoctorProfessionalLicense.trim() : clinic.getResponsibleDoctorProfessionalLicense());
        if (materialReservationLeadDays != null) {
            if (materialReservationLeadDays < 0) {
                throw new IllegalArgumentException("El plazo de reserva de material no puede ser negativo.");
            }
            clinic.setMaterialReservationLeadDays(materialReservationLeadDays);
        }
        clinic.setUpdatedAt(LocalDateTime.now());

        return clinicRepository.save(clinic);
    }

    @Override
    public Clinic updateSpecialty(UUID actingUserId, UUID clinicId, ClinicSpecialty specialty) {
        if (specialty == null) {
            throw new IllegalArgumentException("La especialidad de la clínica es obligatoria.");
        }

        Clinic clinic = clinicRepository.findById(clinicId)
                .orElseThrow(() -> new IllegalArgumentException("Clínica no encontrada"));

        boolean isOwner = clinic.getOwnerUserId().equals(actingUserId);
        if (!isOwner && !permissionChecker.hasPermission(clinicId, actingUserId, StaffPermission.MANAGE_CLINIC)) {
            throw new ClinicAccessDeniedException(
                    "No tienes permiso para cambiar la especialidad de esta clínica.");
        }

        clinic.setSpecialty(specialty);
        clinic.setUpdatedAt(LocalDateTime.now());
        return clinicRepository.save(clinic);
    }

    @Override
    public Clinic getClinic(UUID ownerUserId, UUID clinicId) {
        Clinic clinic = clinicRepository.findById(clinicId)
                .orElseThrow(() -> new IllegalArgumentException("Clínica no encontrada"));

        boolean isOwner = clinic.getOwnerUserId().equals(ownerUserId);
        boolean isStaff = clinicStaffRepository.findByClinicIdAndUserId(clinicId, ownerUserId)
                .map(com.jclinical.staff.domain.model.ClinicStaff::isActive)
                .orElse(false);

        if (!isOwner && !isStaff) {
            throw new IllegalStateException("No tienes permisos para acceder a esta clínica");
        }

        return clinic;
    }

    @Override
    public List<Clinic> getClinicsByOwner(UUID ownerUserId) {
        List<Clinic> owned = clinicRepository.findByOwnerUserId(ownerUserId);
        
        List<Clinic> memberClinics = clinicStaffRepository.findByUserId(ownerUserId).stream()
                .filter(com.jclinical.staff.domain.model.ClinicStaff::isActive)
                .map(staff -> clinicRepository.findById(staff.getClinicId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        java.util.Map<UUID, Clinic> combined = new java.util.LinkedHashMap<>();
        for (Clinic c : owned) {
            combined.put(c.getId(), c);
        }
        for (Clinic c : memberClinics) {
            combined.put(c.getId(), c);
        }
        return new java.util.ArrayList<>(combined.values());
    }

    @Override
    public Optional<ClinicSettings> getSettings(UUID clinicId) {
        return clinicRepository.findById(clinicId)
                .map(clinic -> new ClinicSettings(
                        clinic.getId(),
                        clinic.getMaterialReservationLeadDays() != null ? clinic.getMaterialReservationLeadDays() : 3,
                        clinic.getSpecialty() != null ? clinic.getSpecialty() : ClinicSpecialty.SIN_CONFIGURAR));
    }

    @Override
    public Optional<ClinicPublicProfile> getPublicProfile(UUID clinicId) {
        return clinicRepository.findById(clinicId)
                .map(clinic -> new ClinicPublicProfile(clinic.getId(), clinic.getName(), publicAddress(clinic),
                        blankToNull(clinic.getPhone()), blankToNull(clinic.getEmail()),
                        blankToNull(clinic.getPrivacyNoticeUrl())));
    }

    /** "Calle, Col. Colonia, Municipio, Estado, C.P. 00000", sin las partes que falten. */
    private static String publicAddress(Clinic clinic) {
        List<String> parts = new ArrayList<>();
        addIfPresent(parts, clinic.getAddressStreet(), "");
        addIfPresent(parts, clinic.getAddressColonia(), "Col. ");
        addIfPresent(parts, clinic.getAddressMunicipality(), "");
        addIfPresent(parts, clinic.getAddressState(), "");
        addIfPresent(parts, clinic.getAddressZip(), "C.P. ");
        return parts.isEmpty() ? null : String.join(", ", parts);
    }

    private static void addIfPresent(List<String> parts, String value, String prefix) {
        String clean = blankToNull(value);
        if (clean != null) {
            parts.add(prefix + clean);
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
