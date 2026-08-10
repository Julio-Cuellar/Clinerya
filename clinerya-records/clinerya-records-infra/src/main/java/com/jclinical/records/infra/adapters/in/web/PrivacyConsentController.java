package com.jclinical.records.infra.adapters.in.web;

import com.jclinical.records.domain.model.PrivacyConsent;
import com.jclinical.records.domain.ports.in.ManagePrivacyConsentUseCase;
import com.jclinical.records.domain.ports.in.ManagePrivacyConsentUseCase.SignConsentCommand;
import com.jclinical.records.infra.adapters.in.web.dto.PrivacyConsentResponse;
import com.jclinical.records.infra.adapters.in.web.dto.SignPrivacyConsentRequest;
import com.jclinical.users.infra.security.CurrentUserResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/patients/{patientId}/privacy-consent")
@RequiredArgsConstructor
public class PrivacyConsentController {

    private final ManagePrivacyConsentUseCase privacyConsentUseCase;
    private final CurrentUserResolver currentUserResolver;

    @GetMapping
    public ResponseEntity<PrivacyConsentResponse> getConsent(
            @PathVariable UUID patientId,
            @RequestParam UUID clinicId) {
        var currentUser = currentUserResolver.getCurrentUser();
        return privacyConsentUseCase.getConsent(patientId, clinicId, currentUser.getId())
                .map(consent -> ResponseEntity.ok(toResponse(consent)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<PrivacyConsentResponse> saveConsent(
            @PathVariable UUID patientId,
            @RequestBody SignPrivacyConsentRequest request,
            HttpServletRequest servletRequest) {
        var currentUser = currentUserResolver.getCurrentUser();

        SignConsentCommand command = new SignConsentCommand(
                request.privacyNoticeText(),
                request.signerName(),
                request.signatureImage(),
                servletRequest.getRemoteAddr(),
                servletRequest.getHeader("User-Agent")
        );

        PrivacyConsent consent = privacyConsentUseCase.saveConsent(patientId, request.clinicId(), command, currentUser.getId());
        return ResponseEntity.ok(toResponse(consent));
    }

    private PrivacyConsentResponse toResponse(PrivacyConsent consent) {
        return new PrivacyConsentResponse(
                consent.getId(),
                consent.getPatientId(),
                consent.getClinicId(),
                consent.getPrivacyNoticeText(),
                consent.getDocumentHash(),
                consent.getSignerName(),
                consent.getSignatureImage(),
                consent.getSignatureImageHash(),
                consent.getIpAddress(),
                consent.getUserAgent(),
                consent.getSignedAt(),
                consent.getCreatedAt()
        );
    }
}
