package com.jclinical.agenda.domain.ports.out;

import com.jclinical.agenda.domain.model.ServicePricing;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * El catalogo de servicios de la clinica, leido por la ruta publica del modulo de tratamientos. La agenda
 * solo lo consulta al agendar: la cita guarda su propia copia del nombre y del precio.
 */
@FunctionalInterface
public interface ServiceCatalogPort {

    /** Vacio si el servicio no existe en la clinica o no esta activo. */
    Optional<ServiceSnapshot> findActiveService(UUID clinicId, UUID serviceId);

    /** {@code price}: el que se cobra si es fijo; referencia (puede faltar) si varia por paciente. */
    record ServiceSnapshot(UUID id, String name, ServicePricing pricing, BigDecimal price, Integer durationMinutes) {}
}
