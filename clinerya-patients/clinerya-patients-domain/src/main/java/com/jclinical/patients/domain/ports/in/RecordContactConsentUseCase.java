package com.jclinical.patients.domain.ports.in;

import com.jclinical.patients.domain.model.ConsentSource;
import com.jclinical.patients.domain.model.Patient;

import java.util.UUID;

public interface RecordContactConsentUseCase {

    /** Otorga o revoca el consentimiento de contacto de un paciente de la clinica. */
    Patient recordContactConsent(UUID patientId, UUID clinicId, ContactConsentDecision decision, ConsentSource source);

    /**
     * @param textVersion version de {@code ContactConsentText} que se le leyo; obligatoria al otorgar.
     * @param recordedByUserId quien capturo la decision (personal) o null si la dio el paciente por chat.
     */
    record ContactConsentDecision(boolean granted, String textVersion, UUID recordedByUserId) {}
}
