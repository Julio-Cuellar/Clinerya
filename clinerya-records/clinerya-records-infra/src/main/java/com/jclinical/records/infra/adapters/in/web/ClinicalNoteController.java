package com.jclinical.records.infra.adapters.in.web;

import com.jclinical.records.domain.model.ClinicalNote;
import com.jclinical.records.domain.model.ClinicalNoteAddendum;
import com.jclinical.records.domain.model.ClinicalNoteDiagnosis;
import com.jclinical.records.domain.ports.in.ManageClinicalNoteUseCase;
import com.jclinical.records.domain.ports.in.ManageClinicalNoteUseCase.AddendumCommand;
import com.jclinical.records.domain.ports.in.ManageClinicalNoteUseCase.CreateNoteCommand;
import com.jclinical.records.domain.ports.in.ManageClinicalNoteUseCase.DiagnosisEntry;
import com.jclinical.records.domain.ports.in.ManageClinicalNoteUseCase.SignNoteCommand;
import com.jclinical.records.domain.ports.in.ManageClinicalNoteUseCase.UpdateNoteCommand;
import com.jclinical.records.domain.ports.in.ManageRecordAccessLogUseCase;
import com.jclinical.records.domain.ports.out.StaffDirectoryPort;
import com.jclinical.records.infra.adapters.in.web.dto.ClinicalNoteAddendumResponse;
import com.jclinical.records.infra.adapters.in.web.dto.ClinicalNoteDiagnosisResponse;
import com.jclinical.records.infra.adapters.in.web.dto.ClinicalNoteResponse;
import com.jclinical.records.infra.adapters.in.web.dto.CreateClinicalNoteAddendumRequest;
import com.jclinical.records.infra.adapters.in.web.dto.CreateClinicalNoteRequest;
import com.jclinical.records.infra.adapters.in.web.dto.SignClinicalNoteRequest;
import com.jclinical.records.infra.adapters.in.web.dto.UpdateClinicalNoteRequest;
import com.jclinical.users.infra.security.CurrentUserResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
@RequestMapping("/api/v1/patients/{patientId}/clinical-notes")
@RequiredArgsConstructor
@Transactional
public class ClinicalNoteController {

    private final ManageClinicalNoteUseCase noteUseCase;
    private final CurrentUserResolver currentUserResolver;
    private final ManageRecordAccessLogUseCase recordAccessLogUseCase;
    private final StaffDirectoryPort staffDirectory;

    @PostMapping
    public ResponseEntity<ClinicalNoteResponse> createNote(
            @PathVariable UUID patientId,
            @RequestBody CreateClinicalNoteRequest request,
            HttpServletRequest servletRequest) {
        CreateNoteCommand command = new CreateNoteCommand(
                request.subjective(),
                request.objective(),
                request.temperature(),
                request.bloodPressure(),
                request.heartRate(),
                request.respiratoryRate(),
                request.weight(),
                request.height(),
                request.oxygenSaturation(),
                request.assessment(),
                request.plan(),
                request.status()
        );

        var currentUser = currentUserResolver.getCurrentUser();
        ClinicalNote note = noteUseCase.createClinicalNote(patientId, request.clinicId(), request.doctorId(), command, currentUser.getId());

        // Registrar log de escritura
        recordAccessLogUseCase.logAccess(
                request.clinicId(),
                patientId,
                currentUser.getId(),
                displayName(currentUser.getFullName(), currentUser.getEmail()),
                "CLINICAL_NOTE",
                note.getId(),
                "WRITE",
                servletRequest.getRemoteAddr(),
                servletRequest.getHeader("User-Agent")
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(note));
    }

    @GetMapping
    public ResponseEntity<List<ClinicalNoteResponse>> getNotes(
            @PathVariable UUID patientId,
            @RequestParam UUID clinicId,
            HttpServletRequest servletRequest) {
        var currentUser = currentUserResolver.getCurrentUser();
        List<ClinicalNote> notes = noteUseCase.getClinicalNotesByPatient(patientId, clinicId, currentUser.getId());

        // Registrar log de lectura
        recordAccessLogUseCase.logAccess(
                clinicId,
                patientId,
                currentUser.getId(),
                displayName(currentUser.getFullName(), currentUser.getEmail()),
                "CLINICAL_NOTE",
                null,
                "READ",
                servletRequest.getRemoteAddr(),
                servletRequest.getHeader("User-Agent")
        );

        List<ClinicalNoteResponse> responses = notes.stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{noteId}")
    public ResponseEntity<ClinicalNoteResponse> getNote(
            @PathVariable UUID patientId,
            @PathVariable UUID noteId,
            @RequestParam UUID clinicId,
            HttpServletRequest servletRequest) {
        var currentUser = currentUserResolver.getCurrentUser();
        return noteUseCase.getClinicalNote(noteId, patientId, clinicId, currentUser.getId())
                .map(note -> {
                    // Registrar log de lectura
                    recordAccessLogUseCase.logAccess(
                            clinicId,
                            patientId,
                            currentUser.getId(),
                            displayName(currentUser.getFullName(), currentUser.getEmail()),
                            "CLINICAL_NOTE",
                            note.getId(),
                            "READ",
                            servletRequest.getRemoteAddr(),
                            servletRequest.getHeader("User-Agent")
                    );
                    return ResponseEntity.ok(toResponse(note));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{noteId}")
    public ResponseEntity<ClinicalNoteResponse> updateNote(
            @PathVariable UUID patientId,
            @PathVariable UUID noteId,
            @RequestBody UpdateClinicalNoteRequest request,
            HttpServletRequest servletRequest) {
        UpdateNoteCommand command = new UpdateNoteCommand(
                request.subjective(),
                request.objective(),
                request.temperature(),
                request.bloodPressure(),
                request.heartRate(),
                request.respiratoryRate(),
                request.weight(),
                request.height(),
                request.oxygenSaturation(),
                request.assessment(),
                request.plan()
        );

        var currentUser = currentUserResolver.getCurrentUser();
        ClinicalNote note = noteUseCase.updateClinicalNote(noteId, patientId, request.clinicId(), command, currentUser.getId());

        // Registrar log de escritura
        recordAccessLogUseCase.logAccess(
                request.clinicId(),
                patientId,
                currentUser.getId(),
                displayName(currentUser.getFullName(), currentUser.getEmail()),
                "CLINICAL_NOTE",
                note.getId(),
                "WRITE",
                servletRequest.getRemoteAddr(),
                servletRequest.getHeader("User-Agent")
        );

        return ResponseEntity.ok(toResponse(note));
    }

    @PatchMapping("/{noteId}/sign")
    public ResponseEntity<ClinicalNoteResponse> signNote(
            @PathVariable UUID patientId,
            @PathVariable UUID noteId,
            @RequestParam UUID clinicId,
            @RequestBody(required = false) SignClinicalNoteRequest request,
            HttpServletRequest servletRequest) {
        var currentUser = currentUserResolver.getCurrentUser();
        List<DiagnosisEntry> diagnoses = request == null || request.diagnoses() == null
                ? List.of()
                : request.diagnoses().stream()
                        .map(entry -> new DiagnosisEntry(entry.icd10Code(), entry.kind()))
                        .toList();
        SignNoteCommand command = new SignNoteCommand(
                displayName(currentUser.getFullName(), currentUser.getEmail()),
                servletRequest.getRemoteAddr(),
                servletRequest.getHeader("User-Agent"),
                diagnoses
        );
        ClinicalNote note = noteUseCase.signClinicalNote(noteId, patientId, clinicId, currentUser.getId(), command);

        // Registrar log de escritura (firma)
        recordAccessLogUseCase.logAccess(
                clinicId,
                patientId,
                currentUser.getId(),
                displayName(currentUser.getFullName(), currentUser.getEmail()),
                "CLINICAL_NOTE",
                note.getId(),
                "WRITE",
                servletRequest.getRemoteAddr(),
                servletRequest.getHeader("User-Agent")
        );

        return ResponseEntity.ok(toResponse(note));
    }

    @PostMapping("/{noteId}/addenda")
    public ResponseEntity<ClinicalNoteAddendumResponse> addAddendum(
            @PathVariable UUID patientId,
            @PathVariable UUID noteId,
            @RequestBody CreateClinicalNoteAddendumRequest request,
            HttpServletRequest servletRequest) {
        var currentUser = currentUserResolver.getCurrentUser();
        String authorName = displayName(currentUser.getFullName(), currentUser.getEmail());
        AddendumCommand command = new AddendumCommand(
                request.content(),
                authorName,
                servletRequest.getRemoteAddr(),
                servletRequest.getHeader("User-Agent"));
        ClinicalNoteAddendum addendum = noteUseCase.addAddendum(
                noteId, patientId, request.clinicId(), currentUser.getId(), command);

        recordAccessLogUseCase.logAccess(
                request.clinicId(),
                patientId,
                currentUser.getId(),
                authorName,
                "CLINICAL_NOTE",
                noteId,
                "WRITE",
                servletRequest.getRemoteAddr(),
                servletRequest.getHeader("User-Agent")
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(ClinicalNoteAddendumResponse.from(addendum));
    }

    @GetMapping("/{noteId}/addenda")
    public ResponseEntity<List<ClinicalNoteAddendumResponse>> getAddenda(
            @PathVariable UUID patientId,
            @PathVariable UUID noteId,
            @RequestParam UUID clinicId,
            HttpServletRequest servletRequest) {
        var currentUser = currentUserResolver.getCurrentUser();
        List<ClinicalNoteAddendum> addenda = noteUseCase.getAddenda(noteId, patientId, clinicId, currentUser.getId());

        recordAccessLogUseCase.logAccess(
                clinicId,
                patientId,
                currentUser.getId(),
                displayName(currentUser.getFullName(), currentUser.getEmail()),
                "CLINICAL_NOTE",
                noteId,
                "READ",
                servletRequest.getRemoteAddr(),
                servletRequest.getHeader("User-Agent")
        );

        return ResponseEntity.ok(addenda.stream().map(ClinicalNoteAddendumResponse::from).toList());
    }

    @GetMapping("/{noteId}/diagnoses")
    public ResponseEntity<List<ClinicalNoteDiagnosisResponse>> getDiagnoses(
            @PathVariable UUID patientId,
            @PathVariable UUID noteId,
            @RequestParam UUID clinicId) {
        var currentUser = currentUserResolver.getCurrentUser();
        List<ClinicalNoteDiagnosis> diagnoses = noteUseCase.getDiagnoses(noteId, patientId, clinicId, currentUser.getId());
        return ResponseEntity.ok(diagnoses.stream().map(ClinicalNoteDiagnosisResponse::from).toList());
    }

    private ClinicalNoteResponse toResponse(ClinicalNote note) {
        String doctorName = staffDirectory.staffName(note.getDoctorId(), note.getClinicId()).orElse(null);
        String signedByName = note.getSignedByUserId() == null
                ? null
                : staffDirectory.userName(note.getSignedByUserId(), note.getClinicId()).orElse(null);
        return new ClinicalNoteResponse(
                note.getId(),
                note.getPatientId(),
                note.getClinicId(),
                note.getDoctorId(),
                doctorName,
                note.getSubjective(),
                note.getObjective(),
                note.getVitalSigns(),
                note.getAssessment(),
                note.getPlan(),
                note.getStatus(),
                note.getAuthoredByExternalUserId(),
                note.getSignedAt(),
                note.getSignedByUserId(),
                signedByName,
                note.getDocumentHash(),
                note.getCreatedAt(),
                note.getUpdatedAt()
        );
    }

    private String displayName(String fullName, String email) {
        if (fullName != null && !fullName.isBlank()) {
            return fullName.trim();
        }
        return email;
    }
}
