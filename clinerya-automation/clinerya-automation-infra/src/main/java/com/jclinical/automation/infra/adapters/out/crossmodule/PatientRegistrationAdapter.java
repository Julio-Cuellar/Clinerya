package com.jclinical.automation.infra.adapters.out.crossmodule;

import com.jclinical.automation.domain.ports.out.PatientRegistrationPort;
import com.jclinical.patients.domain.ports.in.RecordContactConsentUseCase;
import com.jclinical.patients.domain.ports.in.RegisterPatientUseCase;

import java.util.UUID;

public class PatientRegistrationAdapter implements PatientRegistrationPort {

    public PatientRegistrationAdapter(RegisterPatientUseCase registerPatient, RecordContactConsentUseCase contactConsent) {
    }

    @Override
    public ConsentText consentText() {
        throw new UnsupportedOperationException("pendiente");
    }

    @Override
    public UUID register(NewPatient patient) {
        throw new UnsupportedOperationException("pendiente");
    }
}
