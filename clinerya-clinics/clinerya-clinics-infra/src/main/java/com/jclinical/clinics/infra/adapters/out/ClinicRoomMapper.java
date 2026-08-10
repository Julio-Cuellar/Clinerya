package com.jclinical.clinics.infra.adapters.out;

import com.jclinical.clinics.domain.model.ClinicRoom;
import com.jclinical.clinics.infra.adapters.in.web.dto.ClinicRoomResponse;
public interface ClinicRoomMapper {
    ClinicRoomEntity toEntity(ClinicRoom domain);
    ClinicRoom toDomain(ClinicRoomEntity entity);
    ClinicRoomResponse toResponse(ClinicRoom domain);
}
