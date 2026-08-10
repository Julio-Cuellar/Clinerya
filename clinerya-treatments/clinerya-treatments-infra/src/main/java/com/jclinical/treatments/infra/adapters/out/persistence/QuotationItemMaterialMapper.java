package com.jclinical.treatments.infra.adapters.out.persistence;

import com.jclinical.treatments.domain.model.QuotationItemMaterial;

public interface QuotationItemMaterialMapper {

    QuotationItemMaterialEntity toEntity(QuotationItemMaterial domain);

    QuotationItemMaterial toDomain(QuotationItemMaterialEntity entity);
}
