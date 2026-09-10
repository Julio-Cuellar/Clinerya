package com.jclinical.records.infra.adapters.in.web;

import com.jclinical.core.security.ClinicAccessDeniedException;
import com.jclinical.core.security.ClinicMembershipPort;
import com.jclinical.records.domain.model.MedicalHistoryTemplate;
import com.jclinical.records.domain.ports.in.ManageHistoryTemplateUseCase;
import com.jclinical.records.domain.ports.in.ManageHistoryTemplateUseCase.CreateTemplateCommand;
import com.jclinical.records.domain.ports.in.ManageHistoryTemplateUseCase.UpdateTemplateCommand;
import com.jclinical.core.security.PatientAccessAuthorizationPort;
import com.jclinical.records.infra.adapters.in.web.dto.CreateHistoryTemplateRequest;
import com.jclinical.records.infra.adapters.in.web.dto.HistoryTemplateResponse;
import com.jclinical.records.infra.adapters.in.web.dto.UpdateHistoryTemplateRequest;
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

/**
 * No cubierto por el interceptor global de pertenencia a clínica (ver ClinicAccessWebConfig):
 * la lectura de plantillas también debe estar disponible para especialistas externos con
 * acceso otorgado a un paciente puntual, aunque no sean staff de esta clínica.
 */
@RestController
@RequestMapping("/api/v1/clinics/{clinicId}/history-templates")
@RequiredArgsConstructor
public class HistoryTemplateController {

    private final ManageHistoryTemplateUseCase templateUseCase;
    private final CurrentUserResolver currentUserResolver;
    private final ClinicMembershipPort clinicMembershipPort;
    private final PatientAccessAuthorizationPort accessAuthorizationPort;

    @PostMapping
    public ResponseEntity<HistoryTemplateResponse> createTemplate(
            @PathVariable UUID clinicId,
            @RequestBody CreateHistoryTemplateRequest request) {
        requireStaff(clinicId);
        CreateTemplateCommand command = new CreateTemplateCommand(
                request.name(),
                request.description(),
                request.schemaJson()
        );
        MedicalHistoryTemplate template = templateUseCase.createTemplate(clinicId, command);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(template));
    }

    @GetMapping
    public ResponseEntity<List<HistoryTemplateResponse>> getTemplates(
            @PathVariable UUID clinicId,
            @RequestParam(required = false) UUID patientId) {
        requireReadAccess(clinicId, patientId);
        List<HistoryTemplateResponse> responses = templateUseCase.getTemplatesByClinic(clinicId).stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{templateId}")
    public ResponseEntity<HistoryTemplateResponse> getTemplate(
            @PathVariable UUID clinicId,
            @PathVariable UUID templateId,
            @RequestParam(required = false) UUID patientId) {
        requireReadAccess(clinicId, patientId);
        return templateUseCase.getTemplate(templateId, clinicId)
                .map(template -> ResponseEntity.ok(toResponse(template)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{templateId}")
    public ResponseEntity<HistoryTemplateResponse> updateTemplate(
            @PathVariable UUID clinicId,
            @PathVariable UUID templateId,
            @RequestBody UpdateHistoryTemplateRequest request) {
        requireStaff(clinicId);
        UpdateTemplateCommand command = new UpdateTemplateCommand(
                request.name(),
                request.description(),
                request.schemaJson(),
                request.active()
        );
        MedicalHistoryTemplate template = templateUseCase.updateTemplate(templateId, clinicId, command);
        return ResponseEntity.ok(toResponse(template));
    }

    @DeleteMapping("/{templateId}")
    public ResponseEntity<Void> deleteTemplate(
            @PathVariable UUID clinicId,
            @PathVariable UUID templateId) {
        requireStaff(clinicId);
        templateUseCase.deleteTemplate(templateId, clinicId);
        return ResponseEntity.noContent().build();
    }

    private void requireStaff(UUID clinicId) {
        UUID userId = currentUserResolver.getCurrentUserId();
        if (!clinicMembershipPort.isActiveStaffMember(userId, clinicId)) {
            throw new ClinicAccessDeniedException("No perteneces al personal de esta clínica.");
        }
    }

    private void requireReadAccess(UUID clinicId, UUID patientId) {
        UUID userId = currentUserResolver.getCurrentUserId();
        if (clinicMembershipPort.isActiveStaffMember(userId, clinicId)) {
            return;
        }
        boolean hasExternalAccess = patientId != null
                && accessAuthorizationPort.resolveAccess(userId, clinicId, patientId).level()
                        != PatientAccessAuthorizationPort.AccessLevel.NONE;
        if (!hasExternalAccess) {
            throw new ClinicAccessDeniedException("No tienes acceso a las plantillas de esta clínica.");
        }
    }

    private HistoryTemplateResponse toResponse(MedicalHistoryTemplate template) {
        return new HistoryTemplateResponse(
                template.getId(),
                template.getClinicId(),
                template.getName(),
                template.getDescription(),
                template.getSchemaJson(),
                template.isActive(),
                template.getCreatedAt(),
                template.getUpdatedAt()
        );
    }
}
