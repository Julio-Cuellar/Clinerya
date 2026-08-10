package com.jclinical.agenda.infra.adapters.out.persistence;

import com.jclinical.agenda.domain.model.ClinicSchedule;

public interface ClinicScheduleMapper {

    ClinicScheduleEntity toEntity(ClinicSchedule domain);

    ClinicSchedule toDomain(ClinicScheduleEntity entity);
}
