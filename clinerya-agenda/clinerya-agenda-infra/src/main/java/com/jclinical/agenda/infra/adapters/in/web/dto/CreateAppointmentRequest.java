package com.jclinical.agenda.infra.adapters.in.web.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CreateAppointmentRequest(
        UUID patientId,
        UUID doctorStaffId,
        UUID roomId,
        UUID quotationId,
        UUID quotationItemId,
        List<UUID> quotationItemIds,
        LocalDateTime scheduledStart,
        LocalDateTime scheduledEnd,
        String reason,
        String notes,
        /** Servicio del catalogo (opcional): su duracion fija el fin si no llega y su precio se copia. */
        UUID serviceId
) {}
