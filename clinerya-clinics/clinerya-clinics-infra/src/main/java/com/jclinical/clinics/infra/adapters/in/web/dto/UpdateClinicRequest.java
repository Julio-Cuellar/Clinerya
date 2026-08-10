package com.jclinical.clinics.infra.adapters.in.web.dto;

public record UpdateClinicRequest(
    String name,
    String legalName,
    String rfc,
    String taxRegimeCode,
    String addressStreet,
    String addressColonia,
    String addressMunicipality,
    String addressState,
    String addressZip,
    String phone,
    String email,
    String logoUrl,
    String timezone,
    String privacyNoticeUrl,
    String cofeprisPermitNumber,
    String responsibleDoctorName,
    String responsibleDoctorProfessionalLicense,
    Integer materialReservationLeadDays
) {}
