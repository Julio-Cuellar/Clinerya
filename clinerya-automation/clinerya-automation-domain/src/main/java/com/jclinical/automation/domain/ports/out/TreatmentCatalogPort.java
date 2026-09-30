package com.jclinical.automation.domain.ports.out;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Tratamientos activos de la clinica, leidos del modulo de tratamientos (ruta publica, sin costos internos). */
@FunctionalInterface
public interface TreatmentCatalogPort {

    List<CatalogTreatment> activeTreatments(UUID clinicId);

    /** {@code category} y {@code price} pueden faltar. */
    /**
     * Servicio que el asistente puede ofrecer. {@code fixedPrice}: el precio es lo que cuesta; si no, es
     * referencia "desde" (puede faltar). {@code id} y {@code durationMinutes} sirven para agendarlo.
     */
    record CatalogTreatment(UUID id, String name, String category, String description, boolean fixedPrice,
                            BigDecimal price, Integer durationMinutes) {
        public CatalogTreatment(String name, String category, BigDecimal price) {
            this(null, name, category, null, true, price, null);
        }
    }
}
