package com.jclinical.agenda.infra.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataClinicScheduleRepository extends JpaRepository<ClinicScheduleEntity, UUID> {

    List<ClinicScheduleEntity> findByClinicId(UUID clinicId);

    Optional<ClinicScheduleEntity> findByClinicIdAndDayOfWeek(UUID clinicId, DayOfWeek dayOfWeek);
}
