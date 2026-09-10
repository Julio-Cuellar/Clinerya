package com.jclinical.agenda.domain.ports.in;

import com.jclinical.agenda.domain.model.ClinicSchedule;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public interface ManageClinicScheduleUseCase {

    /** Ruta interna (validacion de horario al crear/reprogramar citas): sin control de permiso. */
    default List<ClinicSchedule> getSchedule(UUID clinicId) {
        return getSchedule(null, clinicId);
    }

    List<ClinicSchedule> getSchedule(UUID actingUserId, UUID clinicId);

    default List<ClinicSchedule> updateSchedule(UUID clinicId, List<DayScheduleCommand> days) {
        return updateSchedule(null, clinicId, days);
    }

    List<ClinicSchedule> updateSchedule(UUID actingUserId, UUID clinicId, List<DayScheduleCommand> days);

    record DayScheduleCommand(
            DayOfWeek dayOfWeek,
            boolean open,
            LocalTime startTime,
            LocalTime endTime
    ) {}
}
