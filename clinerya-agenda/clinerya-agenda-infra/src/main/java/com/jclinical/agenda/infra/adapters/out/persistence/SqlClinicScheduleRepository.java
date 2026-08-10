package com.jclinical.agenda.infra.adapters.out.persistence;

import com.jclinical.agenda.domain.model.ClinicSchedule;
import com.jclinical.agenda.domain.ports.out.ClinicScheduleRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SqlClinicScheduleRepository implements ClinicScheduleRepositoryPort {

    private final SpringDataClinicScheduleRepository springRepository;
    private final ClinicScheduleMapper mapper;

    @Override
    public List<ClinicSchedule> findByClinicId(UUID clinicId) {
        return springRepository.findByClinicId(clinicId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<ClinicSchedule> findByClinicIdAndDayOfWeek(UUID clinicId, DayOfWeek dayOfWeek) {
        return springRepository.findByClinicIdAndDayOfWeek(clinicId, dayOfWeek).map(mapper::toDomain);
    }

    @Override
    public ClinicSchedule save(ClinicSchedule schedule) {
        if (schedule.getId() == null) {
            schedule.setId(UUID.randomUUID());
        }
        ClinicScheduleEntity entity = mapper.toEntity(schedule);
        ClinicScheduleEntity saved = springRepository.save(entity);
        return mapper.toDomain(saved);
    }
}
