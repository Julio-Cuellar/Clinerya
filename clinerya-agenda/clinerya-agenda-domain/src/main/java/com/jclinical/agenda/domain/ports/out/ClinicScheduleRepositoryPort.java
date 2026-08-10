package com.jclinical.agenda.domain.ports.out;

import com.jclinical.agenda.domain.model.ClinicSchedule;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClinicScheduleRepositoryPort {

    List<ClinicSchedule> findByClinicId(UUID clinicId);

    Optional<ClinicSchedule> findByClinicIdAndDayOfWeek(UUID clinicId, DayOfWeek dayOfWeek);

    ClinicSchedule save(ClinicSchedule schedule);
}
