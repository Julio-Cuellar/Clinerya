package com.jclinical.integrations.infra.adapters.out.persistence;

import com.jclinical.integrations.domain.model.CalendarCredentials;
import com.jclinical.integrations.domain.ports.out.CalendarCredentialsRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SqlCalendarCredentialsRepository implements CalendarCredentialsRepositoryPort {

    private final SpringDataStaffCalendarCredentialsRepository springDataRepository;
    private final StaffCalendarCredentialsMapper mapper;

    @Override
    public CalendarCredentials save(CalendarCredentials credentials) {
        StaffCalendarCredentialsEntity saved = springDataRepository.save(mapper.toEntity(credentials));
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<CalendarCredentials> findByClinicIdAndStaffId(UUID clinicId, UUID staffId) {
        return springDataRepository.findByClinicIdAndStaffId(clinicId, staffId).map(mapper::toDomain);
    }

    @Override
    public void deleteByClinicIdAndStaffId(UUID clinicId, UUID staffId) {
        springDataRepository.deleteByClinicIdAndStaffId(clinicId, staffId);
    }

    @Override
    public java.util.List<CalendarCredentials> findAll() {
        return springDataRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(java.util.stream.Collectors.toList());
    }
}

