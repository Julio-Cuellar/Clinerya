package com.jclinical.automation.infra.adapters.in.scheduling;

import com.jclinical.automation.infra.adapters.out.persistence.JdbcRegistrationDraftRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

/** Cada hora borra los borradores de alta abandonados hace mas de un dia (datos personales). */
@Component
@RequiredArgsConstructor
@Slf4j
public class RegistrationDraftCleanupScheduler {

    static final Duration KEEP_ABANDONED = Duration.ofDays(1);

    private final JdbcRegistrationDraftRepository drafts;

    @Scheduled(cron = "${app.automation.registration.cleanup-cron:0 15 * * * *}")
    @Transactional
    public void deleteAbandoned() {
        try {
            int deleted = drafts.deleteStale(LocalDateTime.now().minus(KEEP_ABANDONED));
            if (deleted > 0) {
                log.info(">>>> [AUTOMATIZACION] {} borrador(es) de alta abandonados eliminados", deleted);
            }
        } catch (RuntimeException exception) {
            log.error(">>>> [AUTOMATIZACION] Fallo la limpieza de borradores de alta", exception);
        }
    }
}
