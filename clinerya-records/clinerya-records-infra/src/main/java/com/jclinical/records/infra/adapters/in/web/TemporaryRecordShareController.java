package com.jclinical.records.infra.adapters.in.web;

import com.jclinical.records.domain.model.TemporaryRecordShare;
import com.jclinical.records.domain.ports.in.ManageTemporaryShareUseCase;
import com.jclinical.records.domain.ports.in.ManageTemporaryShareUseCase.ShareLinkView;
import com.jclinical.records.domain.ports.in.ManageTemporaryShareUseCase.SharedRecordSummary;
import com.jclinical.users.infra.security.CurrentUserResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

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
                clinicId, patientId, request.email(), days, requestingUserId);

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

    public record CreateShareRequest(
            String email,
            int daysValid
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
