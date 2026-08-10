package com.jclinical.staff.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorProfile {
    private UUID id;
    private UUID clinicStaffId;
    private UUID clinicId;
    private String cedulaProfesional;
    private String cedulaEspecialidad;
    private String especialidad;
    private String subEspecialidad;
    private String universidadEgreso;
    private Integer anioEgreso;
    private DoctorCredentialStatus credentialStatus;
    private LocalDateTime verifiedAt;
    private UUID verifiedByUserId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Métodos de negocio
    public void verify(UUID adminUserId) {
        if (adminUserId == null) {
            throw new IllegalArgumentException("El ID del administrador verificador no puede ser nulo");
        }
        if (this.cedulaProfesional == null || this.cedulaProfesional.trim().isEmpty()) {
            throw new IllegalStateException("No se puede activar el perfil sin una cédula profesional registrada");
        }
        this.credentialStatus = DoctorCredentialStatus.ACTIVO;
        this.verifiedByUserId = adminUserId;
        this.verifiedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void suspend() {
        this.credentialStatus = DoctorCredentialStatus.SUSPENDIDO;
        this.updatedAt = LocalDateTime.now();
    }
}
