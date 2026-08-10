package com.jclinical.clinics.infra.adapters.out;

import com.jclinical.clinics.domain.model.Clinic;

public interface ClinicMapper {

    ClinicEntity toEntity(Clinic domain);

    Clinic toDomain(ClinicEntity entity);

    com.jclinical.clinics.infra.adapters.in.web.dto.ClinicResponse toResponse(Clinic domain);
}
