package com.jclinical.agenda.infra.adapters.in.web.dto;

import java.time.LocalDateTime;

public record RescheduleAppointmentRequest(
        LocalDateTime scheduledStart,
        LocalDateTime scheduledEnd
) {}
