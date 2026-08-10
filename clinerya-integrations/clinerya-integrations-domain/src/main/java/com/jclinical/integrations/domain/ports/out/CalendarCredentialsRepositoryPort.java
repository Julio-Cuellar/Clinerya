package com.jclinical.integrations.domain.ports.out;

import com.jclinical.integrations.domain.model.CalendarCredentials;

import java.util.Optional;
import java.util.UUID;

public interface CalendarCredentialsRepositoryPort {

    CalendarCredentials save(CalendarCredentials credentials);

    Optional<CalendarCredentials> findByClinicIdAndStaffId(UUID clinicId, UUID staffId);

    void deleteByClinicIdAndStaffId(UUID clinicId, UUID staffId);

    java.util.List<CalendarCredentials> findAll();
}

