package com.jclinical.automation.infra.adapters.out.crossmodule;

import com.jclinical.automation.domain.ports.out.TreatmentCatalogPort;
import com.jclinical.treatments.domain.model.PricingType;
import com.jclinical.treatments.domain.ports.in.PublicTreatmentCatalogUseCase;

import java.util.List;
import java.util.UUID;

/** El catalogo que ve el agente es el que publica tratamientos (activos, sin costos internos). */
public class TreatmentCatalogAdapter implements TreatmentCatalogPort {

    private final PublicTreatmentCatalogUseCase catalog;

    public TreatmentCatalogAdapter(PublicTreatmentCatalogUseCase catalog) {
        this.catalog = catalog;
    }

    /** Solo los servicios que la clinica ofrece por el asistente y estan completos (lo decide tratamientos). */
    @Override
    public List<CatalogTreatment> activeTreatments(UUID clinicId) {
        return catalog.assistantServices(clinicId).stream()
                .map(item -> new CatalogTreatment(item.id(), item.name(), item.category(), item.description(),
                        item.pricingType() != PricingType.VARIES_BY_PATIENT, item.price(), item.durationMinutes()))
                .toList();
    }
}
