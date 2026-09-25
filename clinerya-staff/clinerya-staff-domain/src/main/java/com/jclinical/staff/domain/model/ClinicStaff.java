package com.jclinical.staff.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClinicStaff {
    private UUID id;
    private UUID clinicId;
    private UUID userId;
    private StaffRole role;
    private String employeeCode;
    private LocalDate hireDate;
    private LocalDate endDate;
    private String notes;
    private boolean active;
    /** Solo cuenta para roles administrativos; un DOCTOR atiende pacientes por su rol. */
    private boolean attendsPatients;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public boolean isAdministrative() {
        return role == StaffRole.ADMIN || role == StaffRole.CLINIC_ADMIN;
    }

    /** Quien puede recibir citas, ocupar un consultorio y ver lo clinico de un paciente. */
    public boolean isPractitioner() {
        return role == StaffRole.DOCTOR || (isAdministrative() && attendsPatients);
    }
}
