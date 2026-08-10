package com.jclinical.staff.infra.adapters.out;

import com.jclinical.staff.domain.model.StaffAttendanceEntry;
import com.jclinical.staff.domain.model.StaffAttendanceStatus;
import com.jclinical.staff.domain.ports.out.StaffAttendanceRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SqlStaffAttendanceRepository implements StaffAttendanceRepositoryPort {
    private final SpringDataStaffAttendanceRepository repository;

    @Override
    public StaffAttendanceEntry save(StaffAttendanceEntry entry) {
        return toDomain(repository.save(toEntity(entry)));
    }

    @Override
    public Optional<StaffAttendanceEntry> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<StaffAttendanceEntry> findOpenByStaffId(UUID clinicId, UUID staffId) {
        return repository.findByClinicIdAndStaffIdAndStatus(clinicId, staffId, StaffAttendanceStatus.OPEN)
                .map(this::toDomain);
    }

    @Override
    public List<StaffAttendanceEntry> findByClinicIdAndWorkDateBetween(UUID clinicId, LocalDate from, LocalDate to) {
        return repository.findByClinicIdAndWorkDateBetweenOrderByWorkDateDescClockInAtDesc(clinicId, from, to).stream()
                .map(this::toDomain)
                .toList();
    }

    private StaffAttendanceEntryEntity toEntity(StaffAttendanceEntry entry) {
        return StaffAttendanceEntryEntity.builder()
                .id(entry.getId())
                .clinicId(entry.getClinicId())
                .staffId(entry.getStaffId())
                .workDate(entry.getWorkDate())
                .clockInAt(entry.getClockInAt())
                .clockOutAt(entry.getClockOutAt())
                .status(entry.getStatus())
                .notes(entry.getNotes())
                .createdAt(entry.getCreatedAt())
                .updatedAt(entry.getUpdatedAt())
                .build();
    }

    private StaffAttendanceEntry toDomain(StaffAttendanceEntryEntity entity) {
        return StaffAttendanceEntry.builder()
                .id(entity.getId())
                .clinicId(entity.getClinicId())
                .staffId(entity.getStaffId())
                .workDate(entity.getWorkDate())
                .clockInAt(entity.getClockInAt())
                .clockOutAt(entity.getClockOutAt())
                .status(entity.getStatus())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
