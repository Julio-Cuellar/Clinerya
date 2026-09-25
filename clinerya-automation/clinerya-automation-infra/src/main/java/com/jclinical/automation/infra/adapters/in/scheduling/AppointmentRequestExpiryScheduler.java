package com.jclinical.automation.infra.adapters.in.scheduling;

import com.jclinical.automation.domain.ports.in.ExpireAppointmentRequestsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Barre las solicitudes sin respuesta en 24 h: las vence, libera sus cupos y avisa al paciente. */
@Component
@RequiredArgsConstructor
@Slf4j
public class AppointmentRequestExpiryScheduler {

    private final ExpireAppointmentRequestsUseCase expiry;

    @Scheduled(fixedDelayString = "${app.automation.requests.expiry-check-ms:60000}",
            initialDelayString = "${app.automation.requests.expiry-initial-delay-ms:30000}")
    public void expireOverdue() {
        try {
            int expired = expiry.expireOverdue();
            if (expired > 0) {
                log.info(">>>> [AUTOMATIZACION] {} solicitud(es) de cita vencida(s) sin respuesta", expired);
            }
        } catch (RuntimeException exception) {
            log.error(">>>> [AUTOMATIZACION] Fallo el barrido de solicitudes vencidas", exception);
        }
    }
}
