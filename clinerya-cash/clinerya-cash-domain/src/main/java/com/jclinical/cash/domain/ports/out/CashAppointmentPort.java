package com.jclinical.cash.domain.ports.out;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface CashAppointmentPort {

    List<CompletedAppointmentSnapshot> listCompletedAppointments(UUID clinicId);

    record CompletedAppointmentSnapshot(
            UUID appointmentId,
            UUID patientId,
            UUID doctorStaffId,
            UUID quotationId,
            UUID quotationItemId,
            List<UUID> quotationItemIds,
            String reason,
            LocalDateTime completedAt
    ) {
        public CompletedAppointmentSnapshot(
                UUID appointmentId,
                UUID patientId,
                UUID doctorStaffId,
                UUID quotationId,
                UUID quotationItemId,
                String reason,
                LocalDateTime completedAt) {
            this(appointmentId, patientId, doctorStaffId, quotationId, quotationItemId,
                    quotationItemId == null ? List.of() : List.of(quotationItemId), reason, completedAt);
        }

        public List<UUID> effectiveQuotationItemIds() {
            return quotationItemIds == null || quotationItemIds.isEmpty()
                    ? (quotationItemId == null ? List.of() : List.of(quotationItemId))
                    : List.copyOf(quotationItemIds);
        }
    }
}
