package com.jclinical.agenda.infra.adapters.out.persistence;

import com.jclinical.agenda.domain.model.Appointment;

public interface AppointmentMapper {

    AppointmentEntity toEntity(Appointment domain);

    Appointment toDomain(AppointmentEntity entity);
}
