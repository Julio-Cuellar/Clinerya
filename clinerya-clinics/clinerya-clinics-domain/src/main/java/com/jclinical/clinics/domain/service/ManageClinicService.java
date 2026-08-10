package com.jclinical.clinics.domain.service;

import com.jclinical.clinics.domain.model.*;
import com.jclinical.clinics.domain.ports.in.GetClinicSettingsUseCase;
import com.jclinical.clinics.domain.ports.in.ManageClinicUseCase;
import com.jclinical.clinics.domain.ports.out.ClinicRepositoryPort;
import com.jclinical.staff.domain.ports.out.ClinicStaffRepositoryPort;
import com.jclinical.staff.domain.ports.out.DoctorProfileRepositoryPort;
import com.jclinical.staff.domain.model.ClinicStaff;
import com.jclinical.staff.domain.model.DoctorCredentialStatus;
import com.jclinical.staff.domain.model.DoctorProfile;
import com.jclinical.staff.domain.model.StaffRole;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ManageClinicService implements ManageClinicUseCase, GetClinicSettingsUseCase {

    private final ClinicRepositoryPort clinicRepository;
    private final ClinicStaffRepositoryPort clinicStaffRepository;
    private final DoctorProfileRepositoryPort doctorProfileRepository;

    public ManageClinicService(ClinicRepositoryPort clinicRepository,
                               ClinicStaffRepositoryPort clinicStaffRepository,
                               DoctorProfileRepositoryPort doctorProfileRepository) {
        this.clinicRepository = clinicRepository;
        this.clinicStaffRepository = clinicStaffRepository;
        this.doctorProfileRepository = doctorProfileRepository;
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
            String responsibleDoctorName,
            String responsibleDoctorProfessionalLicense) {

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de la clínica es obligatorio");
        }

        // 1. Crear Clínica activa
        Clinic clinic = Clinic.builder()
                .id(UUID.randomUUID())
                .ownerUserId(ownerUserId)
                .name(name.trim())
                .email(email != null ? email.trim() : null)
                .timezone(timezone != null ? timezone.trim() : "America/Mexico_City")
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
                .responsibleDoctorName(responsibleDoctorName != null ? responsibleDoctorName.trim() : null)
                .responsibleDoctorProfessionalLicense(responsibleDoctorProfessionalLicense != null ? responsibleDoctorProfessionalLicense.trim() : null)
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // 2. El propietario administra la clinica; su perfil medico puede existir aparte.
        ClinicStaff staff = ClinicStaff.builder()
                .id(UUID.randomUUID())
                .clinicId(clinic.getId())
                .userId(ownerUserId)
                .role(StaffRole.CLINIC_ADMIN)
                .active(true)
                .hireDate(LocalDate.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // 3. Crear DoctorProfile pendiente
        DoctorProfile doctorProfile = DoctorProfile.builder()
                .id(UUID.randomUUID())
                .clinicId(clinic.getId())
                .clinicStaffId(staff.getId())
                .credentialStatus(DoctorCredentialStatus.EN_TRAMITE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // 4. Asignar representante legal
        clinic.assignLegalRepresentative(staff.getId());

        // 5. Guardar todo
        clinicRepository.save(clinic);
        clinicStaffRepository.save(staff);
        doctorProfileRepository.save(doctorProfile);

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
                        clinic.getMaterialReservationLeadDays() != null ? clinic.getMaterialReservationLeadDays() : 3));
    }
}
