package com.jclinical.records.infra.adapters.out.persistence;

import com.jclinical.records.domain.model.Prescription;

public interface PrescriptionMapper {
    PrescriptionEntity toEntity(Prescription domain);
    Prescription toDomain(PrescriptionEntity entity);
}
