package com.jclinical.treatments.domain.ports.in;

import com.jclinical.treatments.domain.model.PricingType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Lo que el catalogo puede mostrarle a un paciente (asistente de WhatsApp): tratamientos activos con
 * nombre, categoria y precio de lista. Nunca costos internos ni materiales. Es una ruta interna: no
 * recibe usuario porque quien pregunta es el paciente; si el precio se comparte lo decide quien llama.
 */
public interface PublicTreatmentCatalogUseCase {

    /** Ordenados por nombre. {@code category} y {@code price} pueden faltar. */
    List<PublicTreatment> activeTreatments(UUID clinicId);

    record PublicTreatment(String name, String category, BigDecimal price) {}

    /**
     * Servicios que el asistente de WhatsApp puede ofrecer: activos, disponibles en el asistente y
     * completos (descripcion y duracion). Ordenados por nombre.
     */
    default List<PublicService> assistantServices(UUID clinicId) {
        throw new UnsupportedOperationException();
    }

    /** Un servicio activo de la clinica, para agendarlo (duracion y precio vigentes). */
    default Optional<PublicService> activeService(UUID clinicId, UUID serviceId) {
        throw new UnsupportedOperationException();
    }

    /** {@code price}: el que se cobra si es fijo; referencia "desde" (puede faltar) si varia por paciente. */
    record PublicService(UUID id, String name, String category, String description, PricingType pricingType,
                         BigDecimal price, Integer durationMinutes) {}
}
