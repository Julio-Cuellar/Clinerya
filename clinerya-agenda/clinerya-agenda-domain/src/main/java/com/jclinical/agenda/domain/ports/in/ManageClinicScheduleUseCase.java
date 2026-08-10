package com.jclinical.agenda.domain.ports.in;

import com.jclinical.agenda.domain.model.ClinicSchedule;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public interface ManageClinicScheduleUseCase {

    List<ClinicSchedule> getSchedule(UUID clinicId);

    List<ClinicSchedule> updateSchedule(UUID clinicId, List<DayScheduleCommand> days);

    record DayScheduleCommand(
            DayOfWeek dayOfWeek,
            boolean open,
            LocalTime startTime,
            LocalTime endTime
    ) {}
}
