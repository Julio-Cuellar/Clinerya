package com.jclinical.records.infra.adapters.in.web;

import com.jclinical.records.domain.model.SharedSection;
import com.jclinical.records.domain.model.TemporaryRecordShare;
import com.jclinical.records.domain.ports.in.ManageTemporaryShareUseCase;
import com.jclinical.records.domain.ports.in.ManageTemporaryShareUseCase.ShareLinkView;
import com.jclinical.records.domain.ports.in.ManageTemporaryShareUseCase.SharedRecordSummary;
import com.jclinical.records.domain.ports.in.ManageTemporaryShareUseCase.SharedStudyContent;
import com.jclinical.users.infra.security.CurrentUserResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class TemporaryRecordShareController {

    private final ManageTemporaryShareUseCase temporaryShareUseCase;
    private final CurrentUserResolver currentUserResolver;

    @PostMapping("/api/v1/clinics/{clinicId}/patients/{patientId}/temporary-shares")
    public ResponseEntity<TemporaryRecordShareResponse> createShareLink(
            @PathVariable UUID clinicId,
            @PathVariable UUID patientId,
            @RequestBody CreateShareRequest request) {

        UUID requestingUserId = currentUserResolver.getCurrentUserId();
        int days = request.daysValid() > 0 ? request.daysValid() : 7;

        TemporaryRecordShare share = temporaryShareUseCase.createShareLink(
                clinicId, patientId, request.email(), days, parseSections(request.sections()), requestingUserId);

        return ResponseEntity.status(HttpStatus.CREATED).body(new TemporaryRecordShareResponse(
                share.getId(),
                share.getClinicId(),
                share.getPatientId(),
                share.getEmail(),
                share.getPlaintextToken(),
                share.getExpiresAt().toString()
        ));
    }

    @GetMapping("/api/v1/clinics/{clinicId}/patients/{patientId}/temporary-shares")
    public ResponseEntity<List<ShareLinkView>> listShares(
            @PathVariable UUID clinicId,
            @PathVariable UUID patientId) {
        return ResponseEntity.ok(temporaryShareUseCase.listActiveShares(
                clinicId, patientId, currentUserResolver.getCurrentUserId()));
    }

    @DeleteMapping("/api/v1/clinics/{clinicId}/patients/{patientId}/temporary-shares/{shareId}")
    public ResponseEntity<Void> revokeShare(
            @PathVariable UUID clinicId,
            @PathVariable UUID patientId,
            @PathVariable UUID shareId) {
        temporaryShareUseCase.revokeShare(clinicId, shareId, currentUserResolver.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Canje del enlace publico. El token va en el cuerpo, no en el query string,
     * para que no quede en logs de proxy ni en la cabecera Referer.
     */
    @PostMapping("/api/v1/public/shared-history")
    public ResponseEntity<SharedRecordSummary> getSharedRecord(
            @RequestBody RedeemShareRequest request,
            HttpServletRequest servletRequest) {
        SharedRecordSummary summary = temporaryShareUseCase.getSharedRecord(
                request.token(),
                servletRequest.getRemoteAddr(),
                servletRequest.getHeader("User-Agent"));
        return ResponseEntity.ok(summary);
    }

    /**
     * Descarga de un estudio expuesto por el enlace. El token va en el cuerpo. Se
     * sirve como {@code attachment} y con {@code nosniff}: un PDF renombrado no
     * debe abrirse ni ejecutarse en nuestro origen.
     */
    @PostMapping("/api/v1/public/shared-history/studies/{attachmentId}/content")
    public ResponseEntity<byte[]> getSharedStudyContent(
            @PathVariable UUID attachmentId,
            @RequestBody RedeemShareRequest request,
            HttpServletRequest servletRequest) {
        SharedStudyContent content = temporaryShareUseCase.getSharedStudyContent(
                request.token(),
                attachmentId,
                servletRequest.getRemoteAddr(),
                servletRequest.getHeader("User-Agent"));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(content.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(content.filename()))
                .header("X-Content-Type-Options", "nosniff")
                .body(content.bytes());
    }

    /** Cabecera segun RFC 6266; el nombre lo eligio quien subio el archivo. */
    private String contentDisposition(String originalFilename) {
        String name = originalFilename == null || originalFilename.isBlank() ? "archivo" : originalFilename;
        String sanitized = name.replaceAll("[\\p{Cntrl}\"\\\\/]", "_");
        String ascii = sanitized.replaceAll("[^\\x20-\\x7E]", "_");
        String utf8 = URLEncoder.encode(sanitized, StandardCharsets.UTF_8).replace("+", "%20");
        return "attachment; filename=\"" + ascii + "\"; filename*=UTF-8''" + utf8;
    }

    /**
     * Nombres invalidos se ignoran; una lista vacia o nula se traduce a "todas las
     * secciones" en el dominio.
     */
    private Set<SharedSection> parseSections(List<String> sections) {
        if (sections == null || sections.isEmpty()) {
            return SharedSection.all();
        }
        Set<SharedSection> parsed = sections.stream()
                .filter(name -> name != null && !name.isBlank())
                .map(name -> {
                    try {
                        return SharedSection.valueOf(name.trim().toUpperCase());
                    } catch (IllegalArgumentException ex) {
                        return null;
                    }
                })
                .filter(section -> section != null)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(SharedSection.class)));
        return parsed.isEmpty() ? SharedSection.all() : parsed;
    }

    public record CreateShareRequest(
            String email,
            int daysValid,
            List<String> sections
    ) {}

    public record RedeemShareRequest(
            String token
    ) {}

    public record TemporaryRecordShareResponse(
            UUID id,
            UUID clinicId,
            UUID patientId,
            String email,
            String token,
            String expiresAt
    ) {}
}
