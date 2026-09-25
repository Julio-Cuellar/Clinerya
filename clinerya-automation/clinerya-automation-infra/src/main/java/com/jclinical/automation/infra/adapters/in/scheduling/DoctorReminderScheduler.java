package com.jclinical.automation.infra.adapters.in.scheduling;

import com.jclinical.automation.domain.ports.in.RemindPendingRequestsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Recuerda al medico las solicitudes que llevan 4 h sin respuesta. */
@Component
@RequiredArgsConstructor
@Slf4j
public class DoctorReminderScheduler {

    private final RemindPendingRequestsUseCase reminders;

    @Scheduled(fixedDelayString = "${app.automation.requests.reminder-check-ms:300000}",
            initialDelayString = "${app.automation.requests.reminder-initial-delay-ms:60000}")
    public void remindPending() {
        try {
            int reminded = reminders.remindPending();
            if (reminded > 0) {
                log.info(">>>> [AUTOMATIZACION] {} recordatorio(s) de solicitudes enviados a medicos", reminded);
            }
        } catch (RuntimeException exception) {
            log.error(">>>> [AUTOMATIZACION] Fallo el barrido de recordatorios a medicos", exception);
        }
    }
}
