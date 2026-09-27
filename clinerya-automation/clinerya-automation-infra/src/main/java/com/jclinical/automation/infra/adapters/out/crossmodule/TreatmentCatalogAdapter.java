package com.jclinical.automation.infra.adapters.out.crossmodule;

import com.jclinical.automation.domain.ports.out.TreatmentCatalogPort;
import com.jclinical.treatments.domain.ports.in.PublicTreatmentCatalogUseCase;

import java.util.List;
import java.util.UUID;

/** El catalogo que ve el agente es el que publica tratamientos (activos, sin costos internos). */
public class TreatmentCatalogAdapter implements TreatmentCatalogPort {

    private final PublicTreatmentCatalogUseCase catalog;

    public TreatmentCatalogAdapter(PublicTreatmentCatalogUseCase catalog) {
        this.catalog = catalog;
    }

    @Override
    public List<CatalogTreatment> activeTreatments(UUID clinicId) {
        return catalog.activeTreatments(clinicId).stream()
                .map(item -> new CatalogTreatment(item.name(), item.category(), item.price()))
                .toList();
    }
}
