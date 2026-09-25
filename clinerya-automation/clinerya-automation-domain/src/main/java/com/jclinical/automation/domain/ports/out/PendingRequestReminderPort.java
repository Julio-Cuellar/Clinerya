package com.jclinical.automation.domain.ports.out;

import com.jclinical.automation.domain.model.AppointmentRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Solicitudes pendientes a las que aun no se les envio recordatorio. */
public interface PendingRequestReminderPort {

    List<AppointmentRequest> findPendingNotRemindedBefore(LocalDateTime createdBefore);

    /** @return false si otro barrido ya la habia marcado (entonces no se envia de nuevo) */
    boolean markReminded(UUID requestId, LocalDateTime at);
}
