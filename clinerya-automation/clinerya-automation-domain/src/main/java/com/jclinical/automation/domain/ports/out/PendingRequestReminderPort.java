package com.jclinical.automation.domain.ports.out;

import com.jclinical.automation.domain.model.AppointmentRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Solicitudes pendientes a las que aun no se les envio recordatorio. */
public interface PendingRequestReminderPort {

    List<AppointmentRequest> findPendingNotRemindedBefore(LocalDateTime createdBefore);

    void markReminded(UUID requestId, LocalDateTime at);
}
