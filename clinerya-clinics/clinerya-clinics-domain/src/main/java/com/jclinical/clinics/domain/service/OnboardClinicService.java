package com.jclinical.clinics.domain.service;

import com.jclinical.clinics.domain.model.*;
import com.jclinical.clinics.domain.ports.in.OnboardClinicUseCase;
import com.jclinical.clinics.domain.ports.out.ClinicRepositoryPort;
import com.jclinical.core.domain.ClinicSpecialty;
import com.jclinical.staff.domain.ports.out.ClinicStaffRepositoryPort;
import com.jclinical.staff.domain.ports.out.DoctorProfileRepositoryPort;
import com.jclinical.staff.domain.model.ClinicStaff;
import com.jclinical.staff.domain.model.DoctorCredentialStatus;
import com.jclinical.staff.domain.model.DoctorProfile;
import com.jclinical.staff.domain.model.StaffRole;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class OnboardClinicService implements OnboardClinicUseCase {

    private final ClinicRepositoryPort clinicRepository;
    private final ClinicStaffRepositoryPort clinicStaffRepository;
    private final DoctorProfileRepositoryPort doctorProfileRepository;

    public OnboardClinicService(ClinicRepositoryPort clinicRepository,
                                ClinicStaffRepositoryPort clinicStaffRepository,
                                DoctorProfileRepositoryPort doctorProfileRepository) {
        this.clinicRepository = clinicRepository;
        this.clinicStaffRepository = clinicStaffRepository;
        this.doctorProfileRepository = doctorProfileRepository;
    }

    @Override
    public void createPendingClinic(UUID ownerUserId, String email, String fullName, String clinicName) {
        // 1. Crear Clínica inactiva
        Clinic clinic = Clinic.builder()
                .id(UUID.randomUUID())
                .ownerUserId(ownerUserId)
                .name(clinicName)
                .email(email)
                .timezone("America/Mexico_City")
                .specialty(ClinicSpecialty.SIN_CONFIGURAR)
                .active(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // 2. Crear miembro Staff inactivo
        ClinicStaff staff = ClinicStaff.builder()
                .id(UUID.randomUUID())
                .clinicId(clinic.getId())
                .userId(ownerUserId)
                .role(StaffRole.CLINIC_ADMIN)
                .active(false)
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
    }

    @Override
    public void activateClinic(UUID ownerUserId) {
        // Buscar todas las clínicas asociadas a este owner y activarlas
        clinicRepository.findByOwnerUserId(ownerUserId).forEach(clinic -> {
            if (!clinic.isActive()) {
                clinic.activate();
                clinicRepository.save(clinic);

                // Activar al miembro de staff asociado
                clinicStaffRepository.findByClinicIdAndUserId(clinic.getId(), ownerUserId).ifPresent(staff -> {
                    if (!staff.isActive()) {
                        staff.setActive(true);
                        clinicStaffRepository.save(staff);
                    }
                });
            }
        });
    }
}
