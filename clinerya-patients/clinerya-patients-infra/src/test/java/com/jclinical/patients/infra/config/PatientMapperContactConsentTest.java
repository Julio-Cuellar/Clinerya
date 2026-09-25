package com.jclinical.patients.infra.config;

import com.jclinical.patients.domain.model.ConsentSource;
import com.jclinical.patients.domain.model.ContactConsent;
import com.jclinical.patients.domain.model.ContactConsentText;
import com.jclinical.patients.domain.model.Patient;
import com.jclinical.patients.infra.adapters.out.PatientEntity;
import com.jclinical.patients.infra.adapters.out.PatientMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** El consentimiento es evidencia legal: no puede perderse ni alterarse al guardar y leer al paciente. */
class PatientMapperContactConsentTest {

    private final PatientMapper mapper = new PatientDomainConfig().patientMapper();

    @Test
    void theConsentSurvivesARoundTripThroughTheDatabase() {
        ContactConsent consent = new ContactConsent(true, ContactConsentText.CURRENT_VERSION,
                ConsentSource.CLINIC_REGISTRATION, UUID.randomUUID(), LocalDateTime.of(2026, 9, 24, 10, 15));
        Patient patient = patient(consent);

        Patient restored = mapper.toDomain(mapper.toEntity(patient));

        assertEquals(consent, restored.getContactConsent());
    }

    @Test
    void aPatientThatWasNeverAskedStaysWithoutConsent() {
        PatientEntity entity = mapper.toEntity(patient(ContactConsent.notRecorded()));

        Patient restored = mapper.toDomain(entity);

        assertFalse(restored.getContactConsent().allowsProactiveContact());
        assertEquals(ContactConsent.notRecorded(), restored.getContactConsent());
    }

    private Patient patient(ContactConsent consent) {
        return Patient.builder()
                .id(UUID.randomUUID())
                .clinicId(UUID.randomUUID())
                .firstName("Ana")
                .lastNamePaterno("Lopez")
                .phone("5512345678")
                .contactConsent(consent)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
