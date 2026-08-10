package com.jclinical.treatments.domain.ports.out;

import com.jclinical.treatments.domain.model.TreatmentCatalogItem;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TreatmentCatalogRepositoryPort {

    TreatmentCatalogItem save(TreatmentCatalogItem item);

    Optional<TreatmentCatalogItem> findByIdAndClinicId(UUID itemId, UUID clinicId);

    List<TreatmentCatalogItem> findByClinicId(UUID clinicId, boolean includeInactive);
}
