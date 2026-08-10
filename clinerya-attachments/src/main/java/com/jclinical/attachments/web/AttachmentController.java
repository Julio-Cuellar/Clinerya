package com.jclinical.attachments.web;

import com.jclinical.attachments.AttachmentEntity;
import com.jclinical.attachments.AttachmentService;
import com.jclinical.core.security.ClinicAccessDeniedException;
import com.jclinical.records.domain.ports.out.PatientAccessAuthorizationPort;
import com.jclinical.records.domain.ports.out.PatientAccessAuthorizationPort.AccessLevel;
import com.jclinical.users.infra.security.CurrentUserResolver;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/patients/{patientId}/attachments")
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentService attachmentService;
    private final CurrentUserResolver currentUserResolver;
    private final PatientAccessAuthorizationPort accessAuthorizationPort;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AttachmentResponse> upload(
            @PathVariable UUID patientId,
            @RequestParam UUID clinicId,
            @RequestParam String elementId,
            @RequestPart("file") MultipartFile file
    ) {
        requireWriteAccess(clinicId, patientId);
        AttachmentEntity saved = attachmentService.store(clinicId, patientId, elementId, file);
        return ResponseEntity.status(201).body(toResponse(saved));
    }

    @GetMapping
    public ResponseEntity<List<AttachmentResponse>> list(
            @PathVariable UUID patientId,
            @RequestParam UUID clinicId,
            @RequestParam String elementId
    ) {
        requireReadAccess(clinicId, patientId);
        List<AttachmentResponse> responses = attachmentService.listByPatientClinicAndElement(patientId, clinicId, elementId).stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{attachmentId}/content")
    public ResponseEntity<byte[]> content(@PathVariable UUID patientId, @PathVariable UUID attachmentId) {
        AttachmentEntity attachment = attachmentService.get(attachmentId, patientId);
        requireReadAccess(attachment.getClinicId(), patientId);
        byte[] bytes = attachmentService.readContent(attachment);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(attachment.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + attachment.getOriginalFilename() + "\"")
                .body(bytes);
    }

    @DeleteMapping("/{attachmentId}")
    public ResponseEntity<Void> delete(@PathVariable UUID patientId, @PathVariable UUID attachmentId) {
        AttachmentEntity attachment = attachmentService.get(attachmentId, patientId);
        requireWriteAccess(attachment.getClinicId(), patientId);
        attachmentService.delete(attachmentId, patientId);
        return ResponseEntity.noContent().build();
    }

    private void requireReadAccess(UUID clinicId, UUID patientId) {
        AccessLevel level = accessAuthorizationPort
                .resolveAccess(currentUserResolver.getCurrentUserId(), clinicId, patientId)
                .level();
        if (level == AccessLevel.NONE) {
            throw new ClinicAccessDeniedException("No tienes acceso a los archivos de este expediente.");
        }
    }

    private void requireWriteAccess(UUID clinicId, UUID patientId) {
        AccessLevel level = accessAuthorizationPort
                .resolveAccess(currentUserResolver.getCurrentUserId(), clinicId, patientId)
                .level();
        if (level != AccessLevel.READ_WRITE) {
            throw new ClinicAccessDeniedException("No tienes permisos para modificar los archivos de este expediente.");
        }
    }

    private AttachmentResponse toResponse(AttachmentEntity entity) {
        return new AttachmentResponse(
                entity.getId(),
                entity.getOriginalFilename(),
                entity.getContentType(),
                entity.getSizeBytes(),
                entity.getCreatedAt()
        );
    }
}
