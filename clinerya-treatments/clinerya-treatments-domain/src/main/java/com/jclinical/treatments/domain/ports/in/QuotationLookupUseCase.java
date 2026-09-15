package com.jclinical.treatments.domain.ports.in;

import com.jclinical.treatments.domain.model.Quotation;

import java.util.Optional;
import java.util.UUID;

/**
 * Lectura de cotizaciones para otros módulos (agenda, caja) que validan contra una
 * cotización sin que exista un usuario en la petición.
 *
 * <p>Deliberadamente separado de {@link ManageQuotationUseCase}: ese exige el usuario que
 * actúa y autoriza contra el expediente del paciente. Este es de solo lectura, de uso
 * interno entre módulos, y <strong>no debe exponerse en ningún controlador</strong>: si un
 * endpoint necesita leer cotizaciones, va por {@code ManageQuotationUseCase}.
 */
public interface QuotationLookupUseCase {

    Optional<Quotation> findQuotationForSystem(UUID quotationId, UUID patientId, UUID clinicId);
}
