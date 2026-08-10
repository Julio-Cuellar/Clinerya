package com.jclinical.integrations.infra.adapters.out.persistence;

import com.jclinical.integrations.domain.model.CalendarCredentials;

public interface StaffCalendarCredentialsMapper {

    StaffCalendarCredentialsEntity toEntity(CalendarCredentials domain);

    CalendarCredentials toDomain(StaffCalendarCredentialsEntity entity);
}
