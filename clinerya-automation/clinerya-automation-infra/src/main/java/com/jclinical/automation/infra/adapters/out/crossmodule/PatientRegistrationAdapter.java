package com.jclinical.automation.infra.adapters.out.crossmodule;

import com.jclinical.automation.domain.ports.out.PatientRegistrationPort;
import com.jclinical.patients.domain.model.ConsentSource;
import com.jclinical.patients.domain.model.ContactConsentText;
import com.jclinical.patients.domain.model.Gender;
import com.jclinical.patients.domain.model.Patient;
import com.jclinical.patients.domain.ports.in.RecordContactConsentUseCase;
import com.jclinical.patients.domain.ports.in.RecordContactConsentUseCase.ContactConsentDecision;
import com.jclinical.patients.domain.ports.in.RegisterPatientUseCase;
import com.jclinical.patients.domain.ports.in.RegisterPatientUseCase.RegisterPatientCommand;

import java.util.UUID;

/**
 * Alta de un paciente nuevo desde WhatsApp por la API publica del modulo de pacientes (nunca sus
 * tablas). El celular se guarda como el resto de los pacientes (10 digitos) para reconocerlo la
 * proxima vez; la autorizacion de contacto queda como dada por el propio paciente en el chat.
 */
public class PatientRegistrationAdapter implements PatientRegistrationPort {

    private final RegisterPatientUseCase registerPatient;
    private final RecordContactConsentUseCase contactConsent;

    public PatientRegistrationAdapter(RegisterPatientUseCase registerPatient, RecordContactConsentUseCase contactConsent) {
        this.registerPatient = registerPatient;
        this.contactConsent = contactConsent;
    }

    @Override
    public ConsentText consentText() {
        return new ConsentText(ContactConsentText.CURRENT_VERSION, ContactConsentText.CURRENT_TEXT);
    }

    @Override
    public UUID register(NewPatient patient) {
        Patient created = registerPatient.registerPatient(new RegisterPatientCommand(patient.clinicId(),
                patient.firstName(), patient.lastNamePaterno(), patient.lastNameMaterno(), null, patient.dateOfBirth(),
                gender(patient.sex()), nationalPhone(patient.phone()), patient.email(), null, null, null, null, null, null));
        contactConsent.recordContactConsent(created.getId(), patient.clinicId(),
                new ContactConsentDecision(true, patient.consentVersion(), null), ConsentSource.WHATSAPP_CHAT);
        return created.getId();
    }

    private static Gender gender(Sex sex) {
        return switch (sex) {
            case FEMALE -> Gender.FEMALE;
            case MALE -> Gender.MALE;
            case OTHER -> Gender.OTHER;
        };
    }

    /** WhatsApp reporta 521 + 10 digitos; en la ficha del paciente van los 10 digitos. */
    static String nationalPhone(String phone) {
        String national = PatientDirectoryAdapter.nationalNumber(phone);
        return national == null ? phone : national;
    }
}
