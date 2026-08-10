package com.jclinical.agenda.domain.service;

import com.jclinical.agenda.domain.model.ClinicSchedule;
import com.jclinical.agenda.domain.ports.in.ManageClinicScheduleUseCase;
import com.jclinical.agenda.domain.ports.out.ClinicScheduleRepositoryPort;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ClinicScheduleService implements ManageClinicScheduleUseCase {

    private final ClinicScheduleRepositoryPort scheduleRepository;

    public ClinicScheduleService(ClinicScheduleRepositoryPort scheduleRepository) {
        this.scheduleRepository = scheduleRepository;
    }

    @Override
    public List<ClinicSchedule> getSchedule(UUID clinicId) {
        List<ClinicSchedule> persisted = scheduleRepository.findByClinicId(clinicId);
        Map<DayOfWeek, ClinicSchedule> byDay = persisted.stream()
                .collect(java.util.stream.Collectors.toMap(ClinicSchedule::getDayOfWeek, s -> s));

        return Arrays.stream(DayOfWeek.values())
                .sorted(Comparator.comparingInt(DayOfWeek::getValue))
                .map(day -> byDay.getOrDefault(day, ClinicSchedule.defaultFor(clinicId, day)))
                .toList();
    }

    @Override
    public List<ClinicSchedule> updateSchedule(UUID clinicId, List<DayScheduleCommand> days) {
        for (DayScheduleCommand day : days) {
            if (day.open() && (day.startTime() == null || day.endTime() == null || !day.startTime().isBefore(day.endTime()))) {
                throw new IllegalArgumentException(
                        "El horario del día " + day.dayOfWeek() + " debe tener hora de inicio anterior a la hora de fin.");
            }

            ClinicSchedule schedule = scheduleRepository.findByClinicIdAndDayOfWeek(clinicId, day.dayOfWeek())
                    .orElseGet(() -> ClinicSchedule.builder().clinicId(clinicId).dayOfWeek(day.dayOfWeek()).build());
            schedule.setOpen(day.open());
            schedule.setStartTime(day.startTime());
            schedule.setEndTime(day.endTime());
            scheduleRepository.save(schedule);
        }
        return getSchedule(clinicId);
    }

    public ClinicSchedule getEffectiveDay(UUID clinicId, DayOfWeek dayOfWeek) {
        return scheduleRepository.findByClinicIdAndDayOfWeek(clinicId, dayOfWeek)
                .orElseGet(() -> ClinicSchedule.defaultFor(clinicId, dayOfWeek));
    }
}
