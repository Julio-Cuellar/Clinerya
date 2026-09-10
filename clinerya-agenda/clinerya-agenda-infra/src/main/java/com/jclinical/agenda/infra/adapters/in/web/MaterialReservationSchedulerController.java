package com.jclinical.agenda.infra.adapters.in.web;

import com.jclinical.agenda.domain.ports.in.ManageMaterialReservationSchedulingUseCase;
import com.jclinical.core.security.PlatformAccessDeniedException;
import com.jclinical.users.domain.model.User;
import com.jclinical.users.infra.security.CurrentUserResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoint operativo para forzar manualmente el job de reserva de material
 * (normalmente corre solo cada 30 min vía @Scheduled). Útil para soporte/pruebas.
 *
 * No es un endpoint de clinica: antes cualquier autenticado podia dispararlo. Se
 * restringe a administradores de plataforma, igual que /api/v1/system-configs.
 */
@RestController
@RequestMapping("/api/v1/internal/scheduler/material-reservations")
@RequiredArgsConstructor
public class MaterialReservationSchedulerController {

    private final ManageMaterialReservationSchedulingUseCase schedulingUseCase;
    private final CurrentUserResolver currentUserResolver;

    @PostMapping("/run")
    public ResponseEntity<Map<String, Integer>> runNow() {
        User user = currentUserResolver.getCurrentUser();
        if (!user.isPlatformAdmin()) {
            throw new PlatformAccessDeniedException(
                    "Se requieren permisos de administrador de plataforma para esta operacion.");
        }
        int processed = schedulingUseCase.processPendingReservations();
        return ResponseEntity.ok(Map.of("processed", processed));
    }
}
