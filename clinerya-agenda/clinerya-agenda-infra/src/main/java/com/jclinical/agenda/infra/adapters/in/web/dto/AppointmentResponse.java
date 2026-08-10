package com.jclinical.agenda.infra.adapters.in.web.dto;

import com.jclinical.agenda.domain.model.AppointmentStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AppointmentResponse(
        UUID id,
        UUID clinicId,
        UUID patientId,
        UUID doctorStaffId,
        UUID roomId,
        UUID quotationId,
        UUID quotationItemId,
        List<UUID> quotationItemIds,
        UUID seriesId,
        LocalDateTime scheduledStart,
        LocalDateTime scheduledEnd,
        String reason,
        String notes,
        String cancellationReason,
        LocalDateTime cancelledAt,
        UUID cancelledByUserId,
        AppointmentStatus status,
        boolean materialsReserved,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
