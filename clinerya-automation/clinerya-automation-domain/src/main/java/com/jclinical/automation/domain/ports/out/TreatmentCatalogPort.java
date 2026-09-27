package com.jclinical.automation.domain.ports.out;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Tratamientos activos de la clinica, leidos del modulo de tratamientos (ruta publica, sin costos internos). */
@FunctionalInterface
public interface TreatmentCatalogPort {

    List<CatalogTreatment> activeTreatments(UUID clinicId);

    /** {@code category} y {@code price} pueden faltar. */
    record CatalogTreatment(String name, String category, BigDecimal price) {}
}
