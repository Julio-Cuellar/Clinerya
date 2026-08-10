package com.jclinical.agenda.infra.adapters.in.web;

import com.jclinical.agenda.domain.ports.in.ManageMaterialReservationSchedulingUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoint operativo para forzar manualmente el job de reserva de material
 * (normalmente corre solo cada 30 min vía @Scheduled). Útil para soporte/pruebas.
 */
@RestController
@RequestMapping("/api/v1/internal/scheduler/material-reservations")
@RequiredArgsConstructor
public class MaterialReservationSchedulerController {

    private final ManageMaterialReservationSchedulingUseCase schedulingUseCase;

    @PostMapping("/run")
    public ResponseEntity<Map<String, Integer>> runNow() {
        int processed = schedulingUseCase.processPendingReservations();
        return ResponseEntity.ok(Map.of("processed", processed));
    }
}
