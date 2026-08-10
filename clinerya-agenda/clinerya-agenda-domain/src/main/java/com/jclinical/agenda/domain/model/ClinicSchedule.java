package com.jclinical.agenda.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClinicSchedule {
    private UUID id;
    private UUID clinicId;
    private DayOfWeek dayOfWeek;
    private boolean open;
    private LocalTime startTime;
    private LocalTime endTime;

    public boolean covers(LocalTime start, LocalTime end) {
        if (!open) {
            return false;
        }
        return !start.isBefore(startTime) && !end.isAfter(endTime);
    }

    public static ClinicSchedule defaultFor(UUID clinicId, DayOfWeek dayOfWeek) {
        boolean isSunday = DayOfWeek.SUNDAY.equals(dayOfWeek);
        return ClinicSchedule.builder()
                .clinicId(clinicId)
                .dayOfWeek(dayOfWeek)
                .open(!isSunday)
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(20, 0))
                .build();
    }
}
