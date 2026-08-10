package com.jclinical.agenda.infra.adapters.in.web.dto;

import com.jclinical.agenda.domain.model.WaitingListEntry;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record WaitingListResponse(
        UUID id,
        UUID clinicId,
        UUID patientId,
        UUID doctorStaffId,
        UUID roomId,
        LocalDate preferredDateFrom,
        LocalDate preferredDateTo,
        String preferredTimeRange,
        String notes,
        WaitingListEntry.Status status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
