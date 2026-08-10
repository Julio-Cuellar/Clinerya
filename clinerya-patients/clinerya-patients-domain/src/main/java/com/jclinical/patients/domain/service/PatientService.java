package com.jclinical.patients.domain.service;

import com.jclinical.patients.domain.model.Patient;
import com.jclinical.patients.domain.ports.in.DeletePatientUseCase;
import com.jclinical.patients.domain.ports.in.GetPatientUseCase;
import com.jclinical.patients.domain.ports.in.RegisterPatientUseCase;
import com.jclinical.patients.domain.ports.in.UpdatePatientUseCase;
import com.jclinical.patients.domain.ports.out.PatientRepositoryPort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PatientService implements RegisterPatientUseCase, GetPatientUseCase, UpdatePatientUseCase, DeletePatientUseCase {

    private final PatientRepositoryPort patientRepository;

    public PatientService(PatientRepositoryPort patientRepository) {
        this.patientRepository = patientRepository;
    }

    @Override
    public Patient registerPatient(RegisterPatientCommand command) {
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
}
