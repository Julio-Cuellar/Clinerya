package com.jclinical.clinics.infra.adapters.in.web.dto;

public record UpdateClinicRoomRequest(
        String name,
        String code,
        String colorHex,
        String description
) {}
