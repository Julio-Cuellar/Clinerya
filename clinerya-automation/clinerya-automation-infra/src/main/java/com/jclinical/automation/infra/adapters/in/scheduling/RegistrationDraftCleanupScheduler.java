package com.jclinical.automation.infra.adapters.in.scheduling;

import com.jclinical.automation.infra.adapters.out.persistence.JdbcPendingActionRepository;
import com.jclinical.automation.infra.adapters.out.persistence.JdbcRegistrationDraftRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Cada hora borra los borradores de alta y las acciones del agente sin confirmar abandonados hace mas
 * de un dia (datos personales).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RegistrationDraftCleanupScheduler {

    static final Duration KEEP_ABANDONED = Duration.ofDays(1);

    private final JdbcRegistrationDraftRepository drafts;
    private final JdbcPendingActionRepository pendingActions;

    @Scheduled(cron = "${app.automation.registration.cleanup-cron:0 15 * * * *}")
    @Transactional
    public void deleteAbandoned() {
        try {
            LocalDateTime cutoff = LocalDateTime.now().minus(KEEP_ABANDONED);
            int deleted = drafts.deleteStale(cutoff) + pendingActions.deleteStale(cutoff);
            if (deleted > 0) {
                log.info(">>>> [AUTOMATIZACION] {} borrador(es) o accion(es) sin confirmar eliminados", deleted);
            }
        } catch (RuntimeException exception) {
            log.error(">>>> [AUTOMATIZACION] Fallo la limpieza de borradores de alta", exception);
        }
    }
}
