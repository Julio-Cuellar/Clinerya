package com.jclinical.treatments.domain.ports.in;

import java.math.BigDecimal;
import java.util.List;
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
}
