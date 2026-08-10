package com.jclinical.records.infra.adapters.out.persistence;

import com.jclinical.records.domain.model.MedicalHistory;

public interface MedicalHistoryMapper {
    MedicalHistoryEntity toEntity(MedicalHistory domain);
    MedicalHistory toDomain(MedicalHistoryEntity entity);
}
