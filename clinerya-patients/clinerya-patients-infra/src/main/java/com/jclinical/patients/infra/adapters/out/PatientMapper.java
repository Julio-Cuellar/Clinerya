package com.jclinical.patients.infra.adapters.out;

import com.jclinical.patients.domain.model.Patient;

public interface PatientMapper {

    PatientEntity toEntity(Patient domain);

    Patient toDomain(PatientEntity entity);
}
