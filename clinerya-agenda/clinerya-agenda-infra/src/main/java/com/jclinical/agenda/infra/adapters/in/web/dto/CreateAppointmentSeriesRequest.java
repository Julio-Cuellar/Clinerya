package com.jclinical.agenda.infra.adapters.in.web.dto;

import com.jclinical.agenda.domain.ports.in.ManageAppointmentsUseCase.RecurrenceFrequency;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CreateAppointmentSeriesRequest(
        UUID patientId,
        UUID doctorStaffId,
        UUID roomId,
        UUID quotationId,
        UUID quotationItemId,
        List<UUID> quotationItemIds,
        LocalDateTime firstScheduledStart,
        LocalDateTime firstScheduledEnd,
        RecurrenceFrequency frequency,
        int repeatCount,
        String reason,
        String notes
) {}
