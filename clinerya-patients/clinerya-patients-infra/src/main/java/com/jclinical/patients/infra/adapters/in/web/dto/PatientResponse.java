package com.jclinical.patients.infra.adapters.in.web.dto;

import com.jclinical.patients.domain.model.Address;
import com.jclinical.patients.domain.model.BloodType;
import com.jclinical.patients.domain.model.EmergencyContact;
import com.jclinical.patients.domain.model.Gender;
import com.jclinical.patients.domain.model.MaritalStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record PatientResponse(
    UUID id,
    UUID clinicId,
    String firstName,
    String lastNamePaterno,
    String lastNameMaterno,
    String curp,
    LocalDate dateOfBirth,
    Gender gender,
    String phone,
    String email,
    String occupation,
    MaritalStatus maritalStatus,
    String nationality,
    BloodType bloodType,
    Address address,
    EmergencyContact emergencyContact,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
