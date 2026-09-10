package com.jclinical.attachments.web;

import com.jclinical.attachments.AttachmentEntity;
import com.jclinical.attachments.AttachmentService;
import com.jclinical.core.security.ClinicAccessDeniedException;
import com.jclinical.core.security.PatientAccessAuthorizationPort;
import com.jclinical.core.security.PatientAccessAuthorizationPort.AccessLevel;
import com.jclinical.users.infra.security.CurrentUserResolver;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
                // 'attachment' en vez de 'inline': un PDF servido inline se abre en el
                // visor del navegador desde nuestro propio origen, y un PDF puede traer
                // JavaScript. Con 'attachment' el archivo se descarga en vez de ejecutarse.
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(attachment.getOriginalFilename()))
                // El nombre lo eligio quien subio el archivo: sin nosniff el navegador
                // podria adivinar el tipo por la extension e ignorar el Content-Type.
                .header("X-Content-Type-Options", "nosniff")
                .body(bytes);
    }

    /**
     * Construye la cabecera segun RFC 6266. El nombre original lo eligio el usuario, asi
     * que antes se inyectaba sin escapar dentro de las comillas: bastaba un {@code "} en
     * el nombre para romper la cabecera.
     */
    private String contentDisposition(String originalFilename) {
        String name = originalFilename == null || originalFilename.isBlank() ? "archivo" : originalFilename;
        // Sin separadores de ruta, comillas ni caracteres de control.
        String sanitized = name.replaceAll("[\\p{Cntrl}\"\\\\/]", "_");
        String ascii = sanitized.replaceAll("[^\\x20-\\x7E]", "_");
        String utf8 = URLEncoder.encode(sanitized, StandardCharsets.UTF_8).replace("+", "%20");
        return "attachment; filename=\"" + ascii + "\"; filename*=UTF-8''" + utf8;
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
