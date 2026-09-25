package com.jclinical.clinics.domain.service;

import com.jclinical.clinics.domain.model.*;
import com.jclinical.clinics.domain.ports.in.OnboardClinicUseCase;
import com.jclinical.clinics.domain.ports.out.ClinicRepositoryPort;
import com.jclinical.core.domain.ClinicSpecialty;
import com.jclinical.staff.domain.ports.out.ClinicStaffRepositoryPort;
import com.jclinical.staff.domain.ports.out.DoctorProfileRepositoryPort;

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

        // 2. Crear al dueño como personal inactivo. Nace solo administrando: si atiende pacientes
        // lo indica, con su cédula, al completar los datos de la clínica (completeSetup).
        ClinicOwnerStaff owner = ClinicOwnerStaff.create(clinic.getId(), ownerUserId, false, false, null);

        // 3. Asignar representante legal
        clinic.assignLegalRepresentative(owner.staff().getId());

        // 4. Guardar todo
        clinicRepository.save(clinic);
        clinicStaffRepository.save(owner.staff());
        owner.doctorProfile().ifPresent(doctorProfileRepository::save);
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
