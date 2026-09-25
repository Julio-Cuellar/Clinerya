package com.jclinical.patients.domain.ports.in;

import com.jclinical.patients.domain.model.Address;
import com.jclinical.patients.domain.model.BloodType;
import com.jclinical.patients.domain.model.EmergencyContact;
import com.jclinical.patients.domain.model.Gender;
import com.jclinical.patients.domain.model.MaritalStatus;
import com.jclinical.patients.domain.model.Patient;

import java.time.LocalDate;
import java.util.UUID;

public interface RegisterPatientUseCase {
    Patient registerPatient(RegisterPatientCommand command);

    record RegisterPatientCommand(
        UUID clinicId,
        String firstName,
        String lastNamePaterno,
        String lastNameMaterno,
        String curp,
        LocalDate dateOfBirth,
        Gender gender,
        String phone,
        String email,
        String occupation,
        MaritalStatus maritalStatus,
        String nationality,
        BloodType bloodType,
        Address address,
        EmergencyContact emergencyContact,
        RecordContactConsentUseCase.ContactConsentDecision contactConsent
    ) {
        /** Alta sin decision de contacto (sembradores, importaciones): el paciente queda sin consentimiento. */
        public RegisterPatientCommand(UUID clinicId, String firstName, String lastNamePaterno, String lastNameMaterno,
                                      String curp, LocalDate dateOfBirth, Gender gender, String phone, String email,
                                      String occupation, MaritalStatus maritalStatus, String nationality,
                                      BloodType bloodType, Address address, EmergencyContact emergencyContact) {
            this(clinicId, firstName, lastNamePaterno, lastNameMaterno, curp, dateOfBirth, gender, phone, email,
                    occupation, maritalStatus, nationality, bloodType, address, emergencyContact, null);
        }
    }
}
