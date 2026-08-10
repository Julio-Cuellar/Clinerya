package com.jclinical.patients.domain.model;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@ToString
public class Patient {
    private final UUID id;
    private final UUID clinicId;
    private final String firstName;
    private final String lastNamePaterno;
    private final String lastNameMaterno;
    private final String curp;
    private final LocalDate dateOfBirth;
    private final Gender gender;
    private final String phone;
    private final String email;
    private final String occupation;
    private final MaritalStatus maritalStatus;
    private final String nationality;
    private final BloodType bloodType;
    private final Address address;
    private final EmergencyContact emergencyContact;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
