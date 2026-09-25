package com.jclinical.clinics.infra.adapters.in.web.dto;

public record CompleteClinicSetupRequest(
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
    String logoUrl,
    String cofeprisPermitNumber,
    String responsibleDoctorName,
    String responsibleDoctorProfessionalLicense,
    Boolean ownerIsResponsibleDoctor,
    Boolean ownerAttendsPatients,
    String ownerCedulaProfesional
) {}
