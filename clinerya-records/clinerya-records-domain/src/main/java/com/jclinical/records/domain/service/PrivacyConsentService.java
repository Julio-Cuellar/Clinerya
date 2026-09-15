package com.jclinical.records.domain.service;

import com.jclinical.core.security.ClinicAccessDeniedException;
import com.jclinical.records.domain.model.PrivacyConsent;
import com.jclinical.records.domain.ports.in.ManagePrivacyConsentUseCase;
import com.jclinical.core.security.PatientAccessAuthorizationPort;
import com.jclinical.core.security.PatientAccessAuthorizationPort.AccessDecision;
import com.jclinical.core.security.PatientAccessAuthorizationPort.AccessLevel;
import com.jclinical.records.domain.ports.out.PatientValidatorPort;
import com.jclinical.records.domain.ports.out.PrivacyConsentRepositoryPort;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
public class PrivacyConsentService implements ManagePrivacyConsentUseCase {

    private final PrivacyConsentRepositoryPort consentRepository;
    private final PatientValidatorPort patientValidator;
    private final PatientAccessAuthorizationPort accessAuthorizationPort;

    @Override
    public PrivacyConsent saveConsent(UUID patientId, UUID clinicId, SignConsentCommand command, UUID currentUserId) {
        authorize(currentUserId, patientId, clinicId, true);

        if (command.signerName() == null || command.signerName().isBlank()) {
            throw new IllegalArgumentException("El nombre del firmante es obligatorio");
        }
        if (command.signatureImage() == null || command.signatureImage().isBlank()) {
            throw new IllegalArgumentException("La firma autógrafa digital es obligatoria");
        }

        String documentHash = sha256(command.privacyNoticeText());
        String signatureImageHash = sha256(command.signatureImage());

        LocalDateTime now = LocalDateTime.now();

        PrivacyConsent consent = consentRepository.findByPatientIdAndClinicId(patientId, clinicId)
                .orElse(PrivacyConsent.builder().id(UUID.randomUUID()).createdAt(now).build());

        consent.setPatientId(patientId);
        consent.setClinicId(clinicId);
        consent.setPrivacyNoticeText(command.privacyNoticeText());
        consent.setDocumentHash(documentHash);
        consent.setSignerName(command.signerName().trim());
        consent.setSignatureImage(command.signatureImage());
        consent.setSignatureImageHash(signatureImageHash);
        consent.setIpAddress(command.ipAddress());
        consent.setUserAgent(command.userAgent());
        consent.setSignedAt(now);

        return consentRepository.save(consent);
    }

    @Override
    public Optional<PrivacyConsent> getConsent(UUID patientId, UUID clinicId, UUID currentUserId) {
        authorize(currentUserId, patientId, clinicId, false);

        return consentRepository.findByPatientIdAndClinicId(patientId, clinicId);
    }

    private void authorize(UUID requestingUserId, UUID patientId, UUID clinicId, boolean requireWrite) {
        if (!patientValidator.existsByIdAndClinicId(patientId, clinicId)) {
            throw new IllegalArgumentException("El paciente no existe en esta clínica.");
        }
        AccessDecision decision = accessAuthorizationPort.resolveAccess(requestingUserId, clinicId, patientId);
        if (decision.level() == AccessLevel.NONE || (requireWrite && decision.level() != AccessLevel.READ_WRITE)) {
            throw new ClinicAccessDeniedException("No tienes acceso a este expediente.");
        }
    }

    private String sha256(String value) {
        if (value == null) return "";
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error computing SHA-256 hash", e);
        }
    }
}
