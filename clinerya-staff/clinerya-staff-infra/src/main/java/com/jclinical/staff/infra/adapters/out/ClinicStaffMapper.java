package com.jclinical.staff.infra.adapters.out;

import com.jclinical.staff.domain.model.ClinicStaff;

public interface ClinicStaffMapper {

    ClinicStaffEntity toEntity(ClinicStaff domain);

    ClinicStaff toDomain(ClinicStaffEntity entity);
}
