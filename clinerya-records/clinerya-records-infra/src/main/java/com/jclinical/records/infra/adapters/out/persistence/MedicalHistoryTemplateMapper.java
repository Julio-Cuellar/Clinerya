package com.jclinical.records.infra.adapters.out.persistence;

import com.jclinical.records.domain.model.MedicalHistoryTemplate;

public interface MedicalHistoryTemplateMapper {
    MedicalHistoryTemplateEntity toEntity(MedicalHistoryTemplate domain);
    MedicalHistoryTemplate toDomain(MedicalHistoryTemplateEntity entity);
}
