package com.jclinical.records.infra.adapters.out.persistence;

import com.jclinical.records.domain.model.ClinicalNote;
import com.jclinical.records.domain.model.VitalSigns;

public interface ClinicalNoteMapper {

    ClinicalNoteEntity toEntity(ClinicalNote domain);

    ClinicalNote toDomain(ClinicalNoteEntity entity);

    default VitalSigns mapVitalSigns(ClinicalNoteEntity entity) {
        if (entity.getVitalTemp() == null && entity.getVitalBp() == null && entity.getVitalHr() == null
                && entity.getVitalRr() == null && entity.getVitalWeight() == null && entity.getVitalHeight() == null
                && entity.getVitalBmi() == null && entity.getVitalO2() == null) {
            return null;
        }
        return new VitalSigns(
                entity.getVitalTemp(),
                entity.getVitalBp(),
                entity.getVitalHr(),
                entity.getVitalRr(),
                entity.getVitalWeight(),
                entity.getVitalHeight(),
                entity.getVitalBmi(),
                entity.getVitalO2()
        );
    }
}
