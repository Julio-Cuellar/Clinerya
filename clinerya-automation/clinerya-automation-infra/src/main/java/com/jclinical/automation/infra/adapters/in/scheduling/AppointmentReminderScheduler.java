package com.jclinical.automation.infra.adapters.in.scheduling;

import com.jclinical.automation.domain.ports.in.AppointmentRemindersUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Envia los recordatorios de cita que ya tocan (plan v2, S6). */
@Component
@RequiredArgsConstructor
@Slf4j
public class AppointmentReminderScheduler {

    private final AppointmentRemindersUseCase reminders;

    @Scheduled(fixedDelayString = "${app.automation.reminders.check-ms:300000}",
            initialDelayString = "${app.automation.reminders.initial-delay-ms:90000}")
    public void sendDue() {
        try {
            int sent = reminders.sendDue();
            if (sent > 0) {
                log.info(">>>> [AUTOMATIZACION] {} recordatorio(s) de cita encolados", sent);
            }
        } catch (RuntimeException exception) {
            log.error(">>>> [AUTOMATIZACION] Fallo el barrido de recordatorios de cita", exception);
        }
    }
}
