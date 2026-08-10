package com.jclinical.cash.infra.adapters.in.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PendingAppointmentChargeResponse(
        UUID appointmentId,
        UUID patientId,
        UUID doctorStaffId,
        UUID quotationId,
        UUID quotationItemId,
        String concept,
        BigDecimal amount,
        LocalDateTime completedAt
) {}
