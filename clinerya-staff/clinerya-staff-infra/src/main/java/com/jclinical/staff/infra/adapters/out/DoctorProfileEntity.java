package com.jclinical.staff.infra.adapters.out;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "doctor_profiles", schema = "staff")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorProfileEntity {

    @Id
    private UUID id;

    @Column(name = "clinic_staff_id", nullable = false, unique = true)
    private UUID clinicStaffId;

    @Column(name = "clinic_id", nullable = false)
    private UUID clinicId;

    @Column(name = "cedula_profesional")
    private String cedulaProfesional;

    @Column(name = "cedula_especialidad")
    private String cedulaEspecialidad;

    private String especialidad;

    @Column(name = "sub_especialidad")
    private String subEspecialidad;

    @Column(name = "universidad_egreso")
    private String universidadEgreso;

    @Column(name = "anio_egreso")
    private Integer anioEgreso;

    @Column(name = "credential_status", nullable = false)
    private String credentialStatus;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "verified_by_user_id")
    private UUID verifiedByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
