package com.jclinical.records.infra.adapters.out.persistence;

import com.jclinical.records.domain.model.MedicalHistoryVersion;

public interface MedicalHistoryVersionMapper {
    MedicalHistoryVersionEntity toEntity(MedicalHistoryVersion domain);
    MedicalHistoryVersion toDomain(MedicalHistoryVersionEntity entity);
}
