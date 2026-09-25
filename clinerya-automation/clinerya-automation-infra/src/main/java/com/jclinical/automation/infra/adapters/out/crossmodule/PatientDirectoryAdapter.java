package com.jclinical.automation.infra.adapters.out.crossmodule;

import com.jclinical.automation.domain.ports.out.PatientDirectoryPort;
import com.jclinical.patients.domain.ports.in.GetPatientUseCase;

import java.util.List;
import java.util.UUID;

public class PatientDirectoryAdapter implements PatientDirectoryPort {

    public PatientDirectoryAdapter(GetPatientUseCase getPatientUseCase) {
    }

    @Override
    public List<PatientContact> findByPhone(UUID clinicId, String phone) {
        return List.of();
    }
}
