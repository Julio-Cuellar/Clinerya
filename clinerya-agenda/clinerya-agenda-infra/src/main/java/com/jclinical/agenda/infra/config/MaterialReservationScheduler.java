package com.jclinical.agenda.infra.config;

import com.jclinical.agenda.domain.service.MaterialReservationSchedulingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Cada 30 minutos revisa las citas ligadas a un plan de tratamiento que todavía no han
 * disparado su reserva de material, y para las que ya entraron en la ventana configurada
 * por clínica ("X días antes de la cita"), publica el evento de reserva hacia Inventario.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MaterialReservationScheduler {

    private final MaterialReservationSchedulingService schedulingService;

    @Scheduled(fixedDelayString = "PT30M", initialDelayString = "PT1M")
    public void run() {
        try {
            schedulingService.processPendingReservations();
        } catch (Exception e) {
            log.error(">>>> [AGENDA] Error procesando reservas de material programadas", e);
        }
    }
}
