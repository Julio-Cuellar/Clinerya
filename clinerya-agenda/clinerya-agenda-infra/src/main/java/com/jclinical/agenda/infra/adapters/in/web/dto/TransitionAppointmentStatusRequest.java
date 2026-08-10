package com.jclinical.agenda.infra.adapters.in.web.dto;

import com.jclinical.agenda.domain.model.AppointmentStatus;

public record TransitionAppointmentStatusRequest(
        AppointmentStatus status,
        String cancellationReason
) {}
