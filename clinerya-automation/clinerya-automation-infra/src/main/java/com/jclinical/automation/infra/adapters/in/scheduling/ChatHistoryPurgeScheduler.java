package com.jclinical.automation.infra.adapters.in.scheduling;

import com.jclinical.automation.domain.ports.in.PurgeChatHistoryUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Una vez al dia borra los mensajes que ya cumplieron la retencion de su clinica. */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChatHistoryPurgeScheduler {

    private final PurgeChatHistoryUseCase purge;

    @Scheduled(cron = "${app.automation.chat.purge-cron:0 30 3 * * *}")
    public void purgeExpired() {
        try {
            int deleted = purge.purgeExpired();
            if (deleted > 0) {
                log.info(">>>> [AUTOMATIZACION] {} mensaje(s) de WhatsApp borrados por retencion", deleted);
            }
        } catch (RuntimeException exception) {
            log.error(">>>> [AUTOMATIZACION] Fallo la purga del historial de WhatsApp", exception);
        }
    }
}
