package com.jclinical.staff.infra.adapters.out;

import com.jclinical.staff.domain.model.DoctorProfile;

public interface DoctorProfileMapper {

    DoctorProfileEntity toEntity(DoctorProfile domain);

    DoctorProfile toDomain(DoctorProfileEntity entity);
}
