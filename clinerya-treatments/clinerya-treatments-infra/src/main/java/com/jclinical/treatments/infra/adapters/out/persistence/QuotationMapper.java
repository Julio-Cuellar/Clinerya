package com.jclinical.treatments.infra.adapters.out.persistence;

import com.jclinical.treatments.domain.model.Quotation;

public interface QuotationMapper {

    QuotationEntity toEntity(Quotation domain);

    Quotation toDomain(QuotationEntity entity);
}
