package com.jclinical.agenda.domain.service;

import com.jclinical.agenda.domain.model.ClinicSchedule;
import com.jclinical.agenda.domain.ports.in.ManageClinicScheduleUseCase;
import com.jclinical.agenda.domain.ports.out.ClinicScheduleRepositoryPort;
import com.jclinical.core.security.ClinicAccessDeniedException;
import com.jclinical.core.security.StaffPermission;
import com.jclinical.core.security.StaffPermissionCheckerPort;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ClinicScheduleService implements ManageClinicScheduleUseCase {

    private final ClinicScheduleRepositoryPort scheduleRepository;
    private final StaffPermissionCheckerPort permissionChecker;

    public ClinicScheduleService(ClinicScheduleRepositoryPort scheduleRepository,
                                 StaffPermissionCheckerPort permissionChecker) {
        this.scheduleRepository = scheduleRepository;
        this.permissionChecker = permissionChecker;
    }

    @Override
    public List<ClinicSchedule> getSchedule(UUID clinicId) {
        return getSchedule(null, clinicId);
    }

    @Override
    public List<ClinicSchedule> getSchedule(UUID actingUserId, UUID clinicId) {
        requirePermission(clinicId, actingUserId, StaffPermission.VIEW_AGENDA,
                "No tienes permiso para consultar el horario de esta clinica.");
        return loadSchedule(clinicId);
    }

    private List<ClinicSchedule> loadSchedule(UUID clinicId) {
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
        return updateSchedule(null, clinicId, days);
    }

    @Override
    public List<ClinicSchedule> updateSchedule(UUID actingUserId, UUID clinicId, List<DayScheduleCommand> days) {
        requirePermission(clinicId, actingUserId, StaffPermission.MANAGE_SCHEDULES,
                "No tienes permiso para modificar el horario de esta clinica.");
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
        return loadSchedule(clinicId);
    }

    public ClinicSchedule getEffectiveDay(UUID clinicId, DayOfWeek dayOfWeek) {
        return scheduleRepository.findByClinicIdAndDayOfWeek(clinicId, dayOfWeek)
                .orElseGet(() -> ClinicSchedule.defaultFor(clinicId, dayOfWeek));
    }

    private void requirePermission(UUID clinicId, UUID actingUserId, StaffPermission permission, String deniedMessage) {
        if (actingUserId == null) {
            return;
        }
        if (!permissionChecker.hasPermission(clinicId, actingUserId, permission)) {
            throw new ClinicAccessDeniedException(deniedMessage);
        }
    }
}
