package com.jclinical.clinics.domain.service;

import com.jclinical.core.domain.CedulaProfesional;
import com.jclinical.staff.domain.model.ClinicStaff;
import com.jclinical.staff.domain.model.DoctorCredentialStatus;
import com.jclinical.staff.domain.model.DoctorProfile;
import com.jclinical.staff.domain.model.StaffRole;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Alta del dueno como personal de su clinica, compartida por el registro (OnboardClinicService) y
 * por "Nueva clinica" (ManageClinicService). El dueno siempre administra; ademas atiende pacientes
 * solo si lo indico al darse de alta, y para eso debe registrar su cedula profesional.
 */
final class ClinicOwnerStaff {

    private final ClinicStaff staff;
    private final DoctorProfile doctorProfile;

    private ClinicOwnerStaff(ClinicStaff staff, DoctorProfile doctorProfile) {
        this.staff = staff;
        this.doctorProfile = doctorProfile;
    }

    static ClinicOwnerStaff create(UUID clinicId, UUID ownerUserId, boolean active,
                                   boolean attendsPatients, String cedulaProfesional) {
        String cedula = attendsPatients ? CedulaProfesional.require(cedulaProfesional) : null;
        LocalDateTime now = LocalDateTime.now();
        ClinicStaff staff = ClinicStaff.builder()
                .id(UUID.randomUUID())
                .clinicId(clinicId)
                .userId(ownerUserId)
                .role(StaffRole.CLINIC_ADMIN)
                .active(active)
                .attendsPatients(attendsPatients)
                .hireDate(LocalDate.now())
                .createdAt(now)
                .updatedAt(now)
                .build();
        DoctorProfile profile = attendsPatients ? DoctorProfile.builder()
                .id(UUID.randomUUID())
                .clinicId(clinicId)
                .clinicStaffId(staff.getId())
                .cedulaProfesional(cedula)
                .credentialStatus(DoctorCredentialStatus.EN_TRAMITE)
                .createdAt(now)
                .updatedAt(now)
                .build() : null;
        return new ClinicOwnerStaff(staff, profile);
    }

    ClinicStaff staff() {
        return staff;
    }

    Optional<DoctorProfile> doctorProfile() {
        return Optional.ofNullable(doctorProfile);
    }
}
