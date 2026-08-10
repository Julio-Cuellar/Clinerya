package com.jclinical.treatments.infra.adapters.out.persistence;

import com.jclinical.treatments.domain.model.TreatmentCatalogMaterial;

public interface TreatmentCatalogMaterialMapper {

    TreatmentCatalogMaterialEntity toEntity(TreatmentCatalogMaterial domain);

    TreatmentCatalogMaterial toDomain(TreatmentCatalogMaterialEntity entity);
}
