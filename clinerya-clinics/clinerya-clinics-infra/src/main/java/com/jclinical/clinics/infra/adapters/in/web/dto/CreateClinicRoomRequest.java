package com.jclinical.clinics.infra.adapters.in.web.dto;

public record CreateClinicRoomRequest(
        String name,
        String code,
        String colorHex,
        String description
) {}
