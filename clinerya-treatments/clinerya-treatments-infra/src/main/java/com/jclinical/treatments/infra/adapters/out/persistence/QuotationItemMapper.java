package com.jclinical.treatments.infra.adapters.out.persistence;

import com.jclinical.treatments.domain.model.QuotationItem;

public interface QuotationItemMapper {

    QuotationItemEntity toEntity(QuotationItem domain);

    QuotationItem toDomain(QuotationItemEntity entity);
}
