package com.jclinical.staff.infra.adapters.out;

import com.jclinical.staff.domain.model.StaffActivityLog;
import com.jclinical.staff.domain.model.StaffActivityType;
import com.jclinical.staff.domain.ports.out.StaffActivityRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SqlStaffActivityRepository implements StaffActivityRepositoryPort {
    private final SpringDataStaffActivityRepository repository;

    @Override
    public StaffActivityLog save(StaffActivityLog activity) {
        return toDomain(repository.save(toEntity(activity)));
    }

    @Override
    public List<StaffActivityLog> findByClinicIdAndOccurredAtBetween(UUID clinicId, LocalDateTime from, LocalDateTime to) {
        return repository.findByClinicIdAndOccurredAtBetweenOrderByOccurredAtDesc(clinicId, from, to).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<StaffActivityLog> findByClinicIdAndStaffIdAndOccurredAtBetween(
            UUID clinicId, UUID staffId, LocalDateTime from, LocalDateTime to) {
        return repository.findByClinicIdAndStaffIdAndOccurredAtBetweenOrderByOccurredAtDesc(clinicId, staffId, from, to).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<StaffActivityLog> findByClinicIdAndTypeAndOccurredAtBetween(
            UUID clinicId, StaffActivityType type, LocalDateTime from, LocalDateTime to) {
        return repository.findByClinicIdAndTypeAndOccurredAtBetweenOrderByOccurredAtDesc(clinicId, type, from, to).stream()
                .map(this::toDomain)
                .toList();
    }

    private StaffActivityLogEntity toEntity(StaffActivityLog activity) {
        return StaffActivityLogEntity.builder()
                .id(activity.getId())
                .clinicId(activity.getClinicId())
                .staffId(activity.getStaffId())
                .type(activity.getType())
                .referenceType(activity.getReferenceType())
                .referenceId(activity.getReferenceId())
                .description(activity.getDescription())
                .amount(activity.getAmount())
                .occurredAt(activity.getOccurredAt())
                .createdAt(activity.getCreatedAt())
                .build();
    }

    private StaffActivityLog toDomain(StaffActivityLogEntity entity) {
        return StaffActivityLog.builder()
                .id(entity.getId())
                .clinicId(entity.getClinicId())
                .staffId(entity.getStaffId())
                .type(entity.getType())
                .referenceType(entity.getReferenceType())
                .referenceId(entity.getReferenceId())
                .description(entity.getDescription())
                .amount(entity.getAmount())
                .occurredAt(entity.getOccurredAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
