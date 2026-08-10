package com.jclinical.patients.infra.adapters.in.web;

import com.jclinical.core.security.ClinicAccessDeniedException;
import com.jclinical.core.security.ClinicMembershipPort;
import com.jclinical.patients.domain.model.Patient;
import com.jclinical.patients.domain.ports.in.DeletePatientUseCase;
import com.jclinical.patients.domain.ports.in.GetPatientUseCase;
import com.jclinical.patients.domain.ports.in.RegisterPatientUseCase;
import com.jclinical.patients.domain.ports.in.RegisterPatientUseCase.RegisterPatientCommand;
import com.jclinical.patients.domain.ports.in.UpdatePatientUseCase;
import com.jclinical.patients.domain.ports.in.UpdatePatientUseCase.UpdatePatientCommand;
import com.jclinical.patients.infra.adapters.in.web.dto.PatientResponse;
import com.jclinical.patients.infra.adapters.in.web.dto.RegisterPatientRequest;
import com.jclinical.patients.infra.adapters.in.web.dto.UpdatePatientRequest;
import com.jclinical.users.infra.security.CurrentUserResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
public class PatientController {

    private final RegisterPatientUseCase registerPatientUseCase;
    private final GetPatientUseCase getPatientUseCase;
    private final UpdatePatientUseCase updatePatientUseCase;
    private final DeletePatientUseCase deletePatientUseCase;
    private final CurrentUserResolver currentUserResolver;
    private final ClinicMembershipPort clinicMembershipPort;

    @PostMapping
    public ResponseEntity<PatientResponse> register(@RequestBody RegisterPatientRequest request) {
        requireMembership(request.clinicId());

        RegisterPatientCommand command = new RegisterPatientCommand(
                request.clinicId(),
                request.firstName(),
                request.lastNamePaterno(),
                request.lastNameMaterno(),
                request.curp(),
                request.dateOfBirth(),
                request.gender(),
                request.phone(),
                request.email(),
                request.occupation(),
                request.maritalStatus(),
                request.nationality(),
                request.bloodType(),
                request.address(),
                request.emergencyContact()
        );

        Patient patient = registerPatientUseCase.registerPatient(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(patient));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PatientResponse> getById(@PathVariable UUID id) {
        return getPatientUseCase.getPatientById(id)
                .map(patient -> {
                    requireMembership(patient.getClinicId());
                    return ResponseEntity.ok(toResponse(patient));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<PatientResponse>> getByClinic(@RequestParam UUID clinicId) {
        requireMembership(clinicId);
        List<PatientResponse> responses = getPatientUseCase.getPatientsByClinic(clinicId).stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PatientResponse> update(@PathVariable UUID id, @RequestBody UpdatePatientRequest request) {
        getPatientUseCase.getPatientById(id).ifPresent(patient -> requireMembership(patient.getClinicId()));

        UpdatePatientCommand command = new UpdatePatientCommand(
                id,
                request.firstName(),
                request.lastNamePaterno(),
                request.lastNameMaterno(),
                request.curp(),
                request.dateOfBirth(),
                request.gender(),
                request.phone(),
                request.email(),
                request.occupation(),
                request.maritalStatus(),
                request.nationality(),
                request.bloodType(),
                request.address(),
                request.emergencyContact()
        );

        return updatePatientUseCase.updatePatient(command)
                .map(patient -> ResponseEntity.ok(toResponse(patient)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        getPatientUseCase.getPatientById(id).ifPresent(patient -> requireMembership(patient.getClinicId()));

        boolean deleted = deletePatientUseCase.deletePatient(id);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    private void requireMembership(UUID clinicId) {
        UUID userId = currentUserResolver.getCurrentUserId();
        if (!clinicMembershipPort.isActiveStaffMember(userId, clinicId)) {
            throw new ClinicAccessDeniedException("No perteneces al personal de esta clínica.");
        }
    }

    private PatientResponse toResponse(Patient patient) {
        return new PatientResponse(
                patient.getId(),
                patient.getClinicId(),
                patient.getFirstName(),
                patient.getLastNamePaterno(),
                patient.getLastNameMaterno(),
                patient.getCurp(),
                patient.getDateOfBirth(),
                patient.getGender(),
                patient.getPhone(),
                patient.getEmail(),
                patient.getOccupation(),
                patient.getMaritalStatus(),
                patient.getNationality(),
                patient.getBloodType(),
                patient.getAddress(),
                patient.getEmergencyContact(),
                patient.getCreatedAt(),
                patient.getUpdatedAt()
        );
    }
}
