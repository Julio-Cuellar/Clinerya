package com.jclinical.cash.domain.ports.in;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ManagePendingAppointmentChargesUseCase {

    List<PendingAppointmentCharge> listPendingCharges(UUID actingUserId, UUID clinicId);

    record PendingAppointmentCharge(
            UUID appointmentId,
            UUID patientId,
            UUID doctorStaffId,
            UUID quotationId,
            UUID quotationItemId,
            String concept,
            BigDecimal amount,
            LocalDateTime completedAt
    ) {}
}
