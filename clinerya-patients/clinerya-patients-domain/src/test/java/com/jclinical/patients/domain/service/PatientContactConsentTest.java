package com.jclinical.patients.domain.service;

import com.jclinical.patients.domain.model.ConsentSource;
import com.jclinical.patients.domain.model.ContactConsent;
import com.jclinical.patients.domain.model.ContactConsentText;
import com.jclinical.patients.domain.model.Patient;
import com.jclinical.patients.domain.ports.in.RecordContactConsentUseCase.ContactConsentDecision;
import com.jclinical.patients.domain.ports.in.RegisterPatientUseCase.RegisterPatientCommand;
import com.jclinical.patients.domain.ports.in.UpdatePatientUseCase.UpdatePatientCommand;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CU-1: quien registra al paciente le lee el texto de autorizacion y marca la casilla. Sin ese
 * registro la automatizacion no puede enviarle ningun mensaje por iniciativa propia, asi que el
 * consentimiento guarda que se leyo (version), quien lo capturo, cuando y por que via.
 */
class PatientContactConsentTest {

    private final InMemoryPatientRepository repository = new InMemoryPatientRepository();
    private final PatientService service = new PatientService(repository);
    private final UUID clinicId = UUID.randomUUID();
    private final UUID receptionistId = UUID.randomUUID();

    @Test
    void registeringWithAuthorizationRecordsWhoWhenAndWhichText() {
        Patient patient = service.registerPatient(command("5512345678", "ana@correo.mx",
                new ContactConsentDecision(true, ContactConsentText.CURRENT_VERSION, receptionistId)));

        ContactConsent consent = patient.getContactConsent();
        assertTrue(consent.granted());
        assertTrue(consent.allowsProactiveContact());
        assertEquals(ContactConsentText.CURRENT_VERSION, consent.textVersion());
        assertEquals(ConsentSource.CLINIC_REGISTRATION, consent.source());
        assertEquals(receptionistId, consent.recordedByUserId());
        assertNotNull(consent.recordedAt());
    }

    @Test
    void anExplicitNoIsAlsoRecorded() {
        Patient patient = service.registerPatient(command("5512345678", null,
                new ContactConsentDecision(false, ContactConsentText.CURRENT_VERSION, receptionistId)));

        ContactConsent consent = patient.getContactConsent();
        assertFalse(consent.allowsProactiveContact());
        assertEquals(receptionistId, consent.recordedByUserId());
        assertNotNull(consent.recordedAt(), "el 'no' tambien queda registrado");
    }

    @Test
    void withoutADecisionThePatientCannotBeContacted() {
        Patient patient = service.registerPatient(command("5512345678", null, null));

        assertFalse(patient.getContactConsent().allowsProactiveContact());
        assertNull(patient.getContactConsent().recordedAt());
    }

    @Test
    void authorizingAgainstAnOutdatedTextIsRejected() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> service.registerPatient(
                command("5512345678", null, new ContactConsentDecision(true, "2020-01-v0", receptionistId))));

        assertTrue(error.getMessage().contains("texto"), error.getMessage());
        assertTrue(repository.patients.isEmpty());
    }

    @Test
    void authorizingWithoutAnyContactChannelIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.registerPatient(
                command(" ", null, new ContactConsentDecision(true, ContactConsentText.CURRENT_VERSION, receptionistId))));
    }

    @Test
    void editingThePatientKeepsTheConsent() {
        Patient patient = service.registerPatient(command("5512345678", null,
                new ContactConsentDecision(true, ContactConsentText.CURRENT_VERSION, receptionistId)));

        Patient updated = service.updatePatient(new UpdatePatientCommand(patient.getId(), "Ana", "Lopez", "Ruiz", null,
                LocalDate.of(1990, 3, 14), null, "5587654321", null, null, null, null, null, null, null)).orElseThrow();

        assertEquals(patient.getContactConsent(), updated.getContactConsent());
    }

    @Test
    void revokingRecordsWhoRevokedAndBlocksContact() {
        Patient patient = service.registerPatient(command("5512345678", null,
                new ContactConsentDecision(true, ContactConsentText.CURRENT_VERSION, receptionistId)));
        UUID doctorId = UUID.randomUUID();

        Patient revoked = service.recordContactConsent(patient.getId(), clinicId,
                new ContactConsentDecision(false, null, doctorId), ConsentSource.CLINIC_UPDATE);

        assertFalse(revoked.getContactConsent().allowsProactiveContact());
        assertEquals(doctorId, revoked.getContactConsent().recordedByUserId());
        assertEquals(ConsentSource.CLINIC_UPDATE, revoked.getContactConsent().source());
        assertEquals("5512345678", revoked.getPhone(), "revocar no toca los demas datos");
    }

    @Test
    void consentCannotBeChangedFromAnotherClinic() {
        Patient patient = service.registerPatient(command("5512345678", null, null));

        assertThrows(IllegalArgumentException.class, () -> service.recordContactConsent(patient.getId(), UUID.randomUUID(),
                new ContactConsentDecision(true, ContactConsentText.CURRENT_VERSION, receptionistId), ConsentSource.CLINIC_UPDATE));
    }

    private RegisterPatientCommand command(String phone, String email, ContactConsentDecision decision) {
        return new RegisterPatientCommand(clinicId, "Ana", "Lopez", "Ruiz", null, LocalDate.of(1990, 3, 14), null,
                phone, email, null, null, null, null, null, null, decision);
    }
}
