package com.jclinical.treatments.infra.adapters.in.web.dto;

import java.util.List;

/**
 * Resultado de sembrar el catálogo sugerido. {@code skipped} cuenta los servicios que ya existían
 * en la clínica: la siembra es idempotente y no los duplica.
 */
public record SeedCatalogResponse(
    int created,
    int skipped,
    List<TreatmentCatalogItemResponse> items
) {}
