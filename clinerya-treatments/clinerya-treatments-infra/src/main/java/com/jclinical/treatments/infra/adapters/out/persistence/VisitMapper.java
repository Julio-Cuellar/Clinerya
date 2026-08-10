package com.jclinical.treatments.infra.adapters.out.persistence;

import com.jclinical.treatments.domain.model.Visit;

public interface VisitMapper {
    VisitEntity toEntity(Visit domain);
    Visit toDomain(VisitEntity entity);
}
