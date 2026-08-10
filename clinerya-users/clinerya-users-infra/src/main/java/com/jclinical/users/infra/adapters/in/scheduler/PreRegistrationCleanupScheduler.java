package com.jclinical.users.infra.adapters.in.scheduler;

import com.jclinical.users.domain.ports.out.UserPreRegistrationRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class PreRegistrationCleanupScheduler {

    private final UserPreRegistrationRepositoryPort preRegistrationRepository;

    @Scheduled(fixedRate = 60000) // Cada 60 segundos (1 minuto)
    public void cleanupExpiredPreRegistrations() {
        log.info(">>>> [LIMPIEZA] Iniciando depuración de pre-registros de usuarios expirados...");
        try {
            preRegistrationRepository.deleteExpiredBefore(LocalDateTime.now());
            log.info(">>>> [LIMPIEZA] Depuración finalizada con éxito.");
        } catch (Exception e) {
            log.error(">>>> [LIMPIEZA] Error al depurar pre-registros expirados: {}", e.getMessage(), e);
        }
    }
}
