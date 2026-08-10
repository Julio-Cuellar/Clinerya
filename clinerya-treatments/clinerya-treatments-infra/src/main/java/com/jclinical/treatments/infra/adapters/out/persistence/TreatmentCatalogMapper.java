package com.jclinical.treatments.infra.adapters.out.persistence;

import com.jclinical.treatments.domain.model.TreatmentCatalogItem;

public interface TreatmentCatalogMapper {

    TreatmentCatalogItemEntity toEntity(TreatmentCatalogItem domain);

    TreatmentCatalogItem toDomain(TreatmentCatalogItemEntity entity);
}
