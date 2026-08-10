package com.jclinical.agenda.infra.adapters.in.web.dto;

import java.time.LocalDate;
import java.util.UUID;

public record AddToWaitingListRequest(
        UUID patientId,
        UUID doctorStaffId,
        UUID roomId,
        LocalDate preferredDateFrom,
        LocalDate preferredDateTo,
        String preferredTimeRange,
        String notes
) {}
