package com.jclinical.clinics.infra.adapters.in.web.dto;

public record CreateClinicRequest(
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
    String cofeprisPermitNumber,
    String responsibleDoctorName,
    String responsibleDoctorProfessionalLicense
) {}
