package com.jclinical.agenda.infra.adapters.in.web.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record DayScheduleResponse(
        DayOfWeek dayOfWeek,
        boolean open,
        LocalTime startTime,
        LocalTime endTime
) {}
