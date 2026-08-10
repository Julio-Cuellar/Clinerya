package com.jclinical.integrations.infra.adapters.out.persistence;

import com.jclinical.integrations.domain.model.ExternalCalendarEvent;
import com.jclinical.integrations.domain.model.ExternalEventStatus;
import com.jclinical.integrations.domain.ports.out.ExternalCalendarEventRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class SqlExternalCalendarEventRepository implements ExternalCalendarEventRepositoryPort {

    private final SpringDataExternalCalendarEventRepository springDataRepository;

    @Override
    public ExternalCalendarEvent save(ExternalCalendarEvent event) {
        ExternalCalendarEventEntity entity = toEntity(event);
        ExternalCalendarEventEntity saved = springDataRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<ExternalCalendarEvent> findById(UUID id) {
        return springDataRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<ExternalCalendarEvent> findByClinicIdAndGoogleEventId(UUID clinicId, String googleEventId) {
        return springDataRepository.findByClinicIdAndGoogleEventId(clinicId, googleEventId).map(this::toDomain);
    }

    @Override
    public List<ExternalCalendarEvent> findByClinicIdAndDateRangeAndStatus(UUID clinicId, LocalDateTime from, LocalDateTime to, ExternalEventStatus status) {
        return springDataRepository.findByClinicIdAndDateRangeAndStatus(clinicId, from, to, status)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(ExternalCalendarEvent event) {
        springDataRepository.delete(toEntity(event));
    }

    private ExternalCalendarEventEntity toEntity(ExternalCalendarEvent domain) {
        if (domain == null) return null;
        return ExternalCalendarEventEntity.builder()
                .id(domain.getId())
                .clinicId(domain.getClinicId())
                .staffId(domain.getStaffId())
                .googleEventId(domain.getGoogleEventId())
                .summary(domain.getSummary())
                .description(domain.getDescription())
                .startTime(domain.getStartTime())
                .endTime(domain.getEndTime())
                .status(domain.getStatus())
                .linkedAppointmentId(domain.getLinkedAppointmentId())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    private ExternalCalendarEvent toDomain(ExternalCalendarEventEntity entity) {
        if (entity == null) return null;
        return ExternalCalendarEvent.builder()
                .id(entity.getId())
                .clinicId(entity.getClinicId())
                .staffId(entity.getStaffId())
                .googleEventId(entity.getGoogleEventId())
                .summary(entity.getSummary())
                .description(entity.getDescription())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .status(entity.getStatus())
                .linkedAppointmentId(entity.getLinkedAppointmentId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
