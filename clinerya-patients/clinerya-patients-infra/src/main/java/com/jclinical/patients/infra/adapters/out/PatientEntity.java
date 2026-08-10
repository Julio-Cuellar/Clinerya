package com.jclinical.patients.infra.adapters.out;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "patients", 
    schema = "patients",
    uniqueConstraints = @UniqueConstraint(columnNames = {"clinic_id", "curp"})
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientEntity {

    @Id
    private UUID id;

    @Column(name = "clinic_id", nullable = false)
    private UUID clinicId;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name_paterno", nullable = false)
    private String lastNamePaterno;

    @Column(name = "last_name_materno")
    private String lastNameMaterno;

    @Column(name = "curp")
    private String curp;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(nullable = false)
    private String gender;

    @Column(nullable = false)
    private String phone;

    private String email;

    private String occupation;

    @Column(name = "marital_status")
    private String maritalStatus;

    private String nationality;

    @Column(name = "blood_type")
    private String bloodType;

    // Domicilio (Address)
    @Column(name = "address_street")
    private String addressStreet;

    @Column(name = "address_outdoor_number")
    private String addressOutdoorNumber;

    @Column(name = "address_indoor_number")
    private String addressIndoorNumber;

    @Column(name = "address_colonia")
    private String addressColonia;

    @Column(name = "address_municipality")
    private String addressMunicipality;

    @Column(name = "address_state")
    private String addressState;

    @Column(name = "address_zip_code")
    private String addressZipCode;

    // Contacto de Emergencia (EmergencyContact)
    @Column(name = "emergency_contact_full_name")
    private String emergencyContactFullName;

    @Column(name = "emergency_contact_relationship")
    private String emergencyContactRelationship;

    @Column(name = "emergency_contact_phone")
    private String emergencyContactPhone;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
