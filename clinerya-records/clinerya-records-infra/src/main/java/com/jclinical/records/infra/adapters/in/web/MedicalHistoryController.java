package com.jclinical.records.infra.adapters.in.web;

import com.jclinical.records.domain.model.MedicalHistory;
import com.jclinical.records.domain.model.MedicalHistoryVersion;
import com.jclinical.records.domain.ports.in.ManageMedicalHistoryUseCase;
import com.jclinical.records.domain.ports.in.ManageMedicalHistoryUseCase.SaveHistoryCommand;
import com.jclinical.records.domain.ports.in.ManageRecordAccessLogUseCase;
import com.jclinical.records.infra.adapters.in.web.dto.MedicalHistoryResponse;
import com.jclinical.records.infra.adapters.in.web.dto.MedicalHistoryVersionResponse;
import com.jclinical.records.infra.signature.MedicalHistorySignatureAuditService;
import com.jclinical.users.infra.security.CurrentUserResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/patients/{patientId}/medical-history")
@RequiredArgsConstructor
public class MedicalHistoryController {

    private final ManageMedicalHistoryUseCase historyUseCase;
    private final CurrentUserResolver currentUserResolver;
    private final MedicalHistorySignatureAuditService signatureAuditService;
    private final ManageRecordAccessLogUseCase recordAccessLogUseCase;

    @GetMapping
    @Transactional
    public ResponseEntity<List<MedicalHistoryResponse>> getMedicalHistories(
            @PathVariable UUID patientId,
            @RequestParam UUID clinicId,
            HttpServletRequest servletRequest) {
        var currentUser = currentUserResolver.getCurrentUser();
        List<MedicalHistory> histories = historyUseCase.getMedicalHistories(patientId, clinicId, currentUser.getId());

        // Registrar log de lectura
        recordAccessLogUseCase.logAccess(
                clinicId,
                patientId,
                currentUser.getId(),
                displayName(currentUser.getFullName(), currentUser.getEmail()),
                "MEDICAL_HISTORY",
                null,
                "READ",
                servletRequest.getRemoteAddr(),
                servletRequest.getHeader("User-Agent")
        );

        List<MedicalHistoryResponse> responses = histories.stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/by-template/{templateId}")
    @Transactional
    public ResponseEntity<MedicalHistoryResponse> getMedicalHistoryByTemplate(
            @PathVariable UUID patientId,
            @PathVariable UUID templateId,
            @RequestParam UUID clinicId,
            HttpServletRequest servletRequest) {
        var currentUser = currentUserResolver.getCurrentUser();
        Optional<MedicalHistory> historyOpt = historyUseCase.getMedicalHistoryByTemplate(patientId, templateId, clinicId, currentUser.getId());

        if (historyOpt.isPresent()) {
            recordAccessLogUseCase.logAccess(
                    clinicId,
                    patientId,
                    currentUser.getId(),
                    displayName(currentUser.getFullName(), currentUser.getEmail()),
                    "MEDICAL_HISTORY",
                    historyOpt.get().getId(),
                    "READ",
                    servletRequest.getRemoteAddr(),
                    servletRequest.getHeader("User-Agent")
            );
        }

        return historyOpt
                .map(history -> ResponseEntity.ok(toResponse(history)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping
    @Transactional
    public ResponseEntity<MedicalHistoryResponse> saveMedicalHistory(
            @PathVariable UUID patientId,
            @RequestBody com.jclinical.records.infra.adapters.in.web.dto.SaveMedicalHistoryRequest request,
            HttpServletRequest servletRequest) {
        var currentUser = currentUserResolver.getCurrentUser();
        String previousAnswersJson = historyUseCase
                .getMedicalHistoryByTemplate(patientId, request.templateId(), request.clinicId(), currentUser.getId())
                .map(MedicalHistory::getAnswersJson)
                .orElse(null);

        SaveHistoryCommand command = new SaveHistoryCommand(
                request.templateId(),
                request.answersJson(),
                displayName(currentUser.getFullName(), currentUser.getEmail()),
                servletRequest.getRemoteAddr(),
                servletRequest.getHeader("User-Agent")
        );

        MedicalHistory history = historyUseCase.saveMedicalHistory(patientId, request.clinicId(), command, currentUser.getId());

        // Registrar log de escritura
        recordAccessLogUseCase.logAccess(
                request.clinicId(),
                patientId,
                currentUser.getId(),
                displayName(currentUser.getFullName(), currentUser.getEmail()),
                "MEDICAL_HISTORY",
                history.getId(),
                "WRITE",
                servletRequest.getRemoteAddr(),
                servletRequest.getHeader("User-Agent")
        );

        signatureAuditService.recordSignatures(
                history,
                previousAnswersJson,
                currentUser.getId(),
                displayName(currentUser.getFullName(), currentUser.getEmail()),
                servletRequest.getRemoteAddr(),
                servletRequest.getHeader("User-Agent")
        );
        return ResponseEntity.ok(toResponse(history));
    }

    @GetMapping("/by-template/{templateId}/versions")
    public ResponseEntity<List<MedicalHistoryVersionResponse>> getHistoryVersions(
            @PathVariable UUID patientId,
            @PathVariable UUID templateId,
            @RequestParam UUID clinicId) {
        var currentUser = currentUserResolver.getCurrentUser();
        List<MedicalHistoryVersionResponse> responses = historyUseCase.getMedicalHistoryVersions(patientId, templateId, clinicId, currentUser.getId()).stream()
                .map(this::toVersionResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/by-template/{templateId}/versions/{versionNumber}")
    public ResponseEntity<MedicalHistoryVersionResponse> getHistoryVersion(
            @PathVariable UUID patientId,
            @PathVariable UUID templateId,
            @PathVariable int versionNumber,
            @RequestParam UUID clinicId) {
        var currentUser = currentUserResolver.getCurrentUser();
        return historyUseCase.getMedicalHistoryVersion(patientId, templateId, versionNumber, clinicId, currentUser.getId())
                .map(version -> ResponseEntity.ok(toVersionResponse(version)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private MedicalHistoryResponse toResponse(MedicalHistory history) {
        return new MedicalHistoryResponse(
                history.getId(),
                history.getPatientId(),
                history.getClinicId(),
                history.getTemplateId(),
                history.getAnswersJson(),
                history.getCreatedAt(),
                history.getUpdatedAt()
        );
    }

    private MedicalHistoryVersionResponse toVersionResponse(MedicalHistoryVersion version) {
        return new MedicalHistoryVersionResponse(
                version.getId(),
                version.getMedicalHistoryId(),
                version.getVersion(),
                version.getAnswersJson(),
                version.getChangedByUserId(),
                version.getChangedByUserName(),
                version.getIpAddress(),
                version.getUserAgent(),
                version.getCreatedAt()
        );
    }

    private String displayName(String fullName, String email) {
        if (fullName != null && !fullName.isBlank()) {
            return fullName.trim();
        }
        return email;
    }
}
