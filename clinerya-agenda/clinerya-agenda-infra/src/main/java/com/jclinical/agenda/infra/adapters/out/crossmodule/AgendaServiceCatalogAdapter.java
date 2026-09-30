package com.jclinical.agenda.infra.adapters.out.crossmodule;

import com.jclinical.agenda.domain.model.ServicePricing;
import com.jclinical.agenda.domain.ports.out.ServiceCatalogPort;
import com.jclinical.treatments.domain.model.PricingType;
import com.jclinical.treatments.domain.ports.in.PublicTreatmentCatalogUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** El servicio y su precio vigente salen de la ruta publica del modulo de tratamientos; aqui solo se traducen. */
@Component
@RequiredArgsConstructor
public class AgendaServiceCatalogAdapter implements ServiceCatalogPort {

    private final PublicTreatmentCatalogUseCase catalog;

    @Override
    public Optional<ServiceSnapshot> findActiveService(UUID clinicId, UUID serviceId) {
        return catalog.activeService(clinicId, serviceId)
                .map(service -> new ServiceSnapshot(service.id(), service.name(),
                        service.pricingType() == PricingType.VARIES_BY_PATIENT ? ServicePricing.VARIES_BY_PATIENT
                                : ServicePricing.FIXED,
                        service.price(), service.durationMinutes()));
    }
}
