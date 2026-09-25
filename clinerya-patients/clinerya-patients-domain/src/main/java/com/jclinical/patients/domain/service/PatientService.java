package com.jclinical.patients.domain.service;

import com.jclinical.patients.domain.model.ConsentSource;
import com.jclinical.patients.domain.model.ContactConsent;
import com.jclinical.patients.domain.model.ContactConsentText;
import com.jclinical.patients.domain.model.Patient;
import com.jclinical.patients.domain.ports.in.DeletePatientUseCase;
import com.jclinical.patients.domain.ports.in.GetPatientUseCase;
import com.jclinical.patients.domain.ports.in.RecordContactConsentUseCase;
import com.jclinical.patients.domain.ports.in.RegisterPatientUseCase;
import com.jclinical.patients.domain.ports.in.UpdatePatientUseCase;
import com.jclinical.patients.domain.ports.out.PatientRepositoryPort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PatientService implements RegisterPatientUseCase, GetPatientUseCase, UpdatePatientUseCase, DeletePatientUseCase,
        RecordContactConsentUseCase {

    private final PatientRepositoryPort patientRepository;

    public PatientService(PatientRepositoryPort patientRepository) {
        this.patientRepository = patientRepository;
    }

    @Override
    public Patient registerPatient(RegisterPatientCommand command) {
        ContactConsent consent = command.contactConsent() == null
                ? ContactConsent.notRecorded()
                : toConsent(command.contactConsent(), ConsentSource.CLINIC_REGISTRATION, command.phone(), command.email());
        if (command.curp() != null && !command.curp().trim().isEmpty()) {
            if (patientRepository.existsByCurpAndClinicId(command.curp().trim(), command.clinicId())) {
                throw new IllegalArgumentException("Ya existe un paciente registrado con el mismo CURP en esta clínica");
            }
        }

        Patient patient = Patient.builder()
                .id(UUID.randomUUID())
                .clinicId(command.clinicId())
                .firstName(command.firstName())
                .lastNamePaterno(command.lastNamePaterno())
                .lastNameMaterno(command.lastNameMaterno())
                .curp(command.curp() != null ? command.curp().trim().toUpperCase() : null)
                .dateOfBirth(command.dateOfBirth())
                .gender(command.gender())
                .phone(command.phone())
                .email(command.email())
                .occupation(command.occupation())
                .maritalStatus(command.maritalStatus())
                .nationality(command.nationality())
                .bloodType(command.bloodType())
                .address(command.address())
                .emergencyContact(command.emergencyContact())
                .contactConsent(consent)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return patientRepository.save(patient);
    }

    @Override
    public Optional<Patient> getPatientById(UUID id) {
        return patientRepository.findById(id);
    }

    @Override
    public List<Patient> getPatientsByClinic(UUID clinicId) {
        return patientRepository.findByClinicId(clinicId);
    }

    @Override
    public Optional<Patient> updatePatient(UpdatePatientCommand command) {
        return patientRepository.findById(command.id()).map(existing -> {
            String normalizedCurp = command.curp() != null ? command.curp().trim().toUpperCase() : null;
            if (normalizedCurp != null && !normalizedCurp.isEmpty() && !normalizedCurp.equals(existing.getCurp())) {
                if (patientRepository.existsByCurpAndClinicId(normalizedCurp, existing.getClinicId())) {
                    throw new IllegalArgumentException("Ya existe un paciente registrado con el mismo CURP en esta clínica");
                }
            }

            Patient patient = Patient.builder()
                    .id(existing.getId())
                    .clinicId(existing.getClinicId())
                    .firstName(command.firstName())
                    .lastNamePaterno(command.lastNamePaterno())
                    .lastNameMaterno(command.lastNameMaterno())
                    .curp(normalizedCurp)
                    .dateOfBirth(command.dateOfBirth())
                    .gender(command.gender())
                    .phone(command.phone())
                    .email(command.email())
                    .occupation(command.occupation())
                    .maritalStatus(command.maritalStatus())
                    .nationality(command.nationality())
                    .bloodType(command.bloodType())
                    .address(command.address())
                    .emergencyContact(command.emergencyContact())
                    .contactConsent(existing.getContactConsent())
                    .createdAt(existing.getCreatedAt())
                    .updatedAt(LocalDateTime.now())
                    .build();

            return patientRepository.save(patient);
        });
    }

    @Override
    public boolean deletePatient(UUID id) {
        if (!patientRepository.existsById(id)) {
            return false;
        }
        patientRepository.deleteById(id);
        return true;
    }

    @Override
    public Patient recordContactConsent(UUID patientId, UUID clinicId, ContactConsentDecision decision, ConsentSource source) {
        if (decision == null || source == null) {
            throw new IllegalArgumentException("La decision de contacto y su origen son obligatorios.");
        }
        Patient existing = patientRepository.findById(patientId)
                .filter(patient -> patient.getClinicId().equals(clinicId))
                .orElseThrow(() -> new IllegalArgumentException("El paciente no existe en esta clínica."));
        ContactConsent consent = toConsent(decision, source, existing.getPhone(), existing.getEmail());
        return patientRepository.save(existing.toBuilder()
                .contactConsent(consent)
                .updatedAt(LocalDateTime.now())
                .build());
    }

    private ContactConsent toConsent(ContactConsentDecision decision, ConsentSource source, String phone, String email) {
        if (decision.granted()) {
            if (!ContactConsentText.CURRENT_VERSION.equals(decision.textVersion())) {
                throw new IllegalArgumentException(
                        "El texto de autorización cambió. Vuelve a leerle al paciente el texto vigente antes de registrarlo.");
            }
            if (isBlank(phone) && isBlank(email)) {
                throw new IllegalArgumentException(
                        "Para autorizar el contacto el paciente debe tener al menos un celular o un correo.");
            }
        }
        return new ContactConsent(decision.granted(), decision.textVersion(), source,
                decision.recordedByUserId(), LocalDateTime.now());
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
