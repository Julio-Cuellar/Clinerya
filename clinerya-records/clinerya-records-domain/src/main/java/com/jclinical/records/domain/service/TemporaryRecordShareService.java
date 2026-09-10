package com.jclinical.records.domain.service;

import com.jclinical.core.security.ClinicAccessDeniedException;
import com.jclinical.records.domain.model.ClinicalNote;
import com.jclinical.records.domain.model.RecordAccessLog;
import com.jclinical.records.domain.model.TemporaryRecordShare;
import com.jclinical.records.domain.ports.in.ManageTemporaryShareUseCase;
import com.jclinical.records.domain.ports.out.ClinicalNoteRepositoryPort;
import com.jclinical.records.domain.ports.out.ClinicLookupPort;
import com.jclinical.core.security.PatientAccessAuthorizationPort;
import com.jclinical.core.security.PatientAccessAuthorizationPort.AccessDecision;
import com.jclinical.core.security.PatientAccessAuthorizationPort.AccessLevel;
import com.jclinical.records.domain.ports.out.PatientLookupPort;
import com.jclinical.records.domain.ports.out.RecordAccessLogOutboxPort;
import com.jclinical.records.domain.ports.out.TemporaryRecordShareRepositoryPort;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

public class TemporaryRecordShareService implements ManageTemporaryShareUseCase {
    private static final SecureRandom TOKEN_RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    private final TemporaryRecordShareRepositoryPort repository;
    private final ClinicalNoteRepositoryPort noteRepository;
    private final PatientLookupPort patientLookup;
    private final ClinicLookupPort clinicLookup;
    private final PatientAccessAuthorizationPort accessAuthorizationPort;
    private final RecordAccessLogOutboxPort accessLogOutbox;

    public TemporaryRecordShareService(TemporaryRecordShareRepositoryPort repository,
                                       ClinicalNoteRepositoryPort noteRepository,
                                       PatientLookupPort patientLookup,
                                       ClinicLookupPort clinicLookup,
                                       PatientAccessAuthorizationPort accessAuthorizationPort,
                                       RecordAccessLogOutboxPort accessLogOutbox) {
        this.repository = repository;
        this.noteRepository = noteRepository;
        this.patientLookup = patientLookup;
        this.clinicLookup = clinicLookup;
        this.accessAuthorizationPort = accessAuthorizationPort;
        this.accessLogOutbox = accessLogOutbox;
    }

    @Override
    public TemporaryRecordShare createShareLink(UUID clinicId, UUID patientId, String email, int daysValid, UUID requestingUserId) {
        requireReadWrite(requestingUserId, clinicId, patientId, "No tienes permisos para compartir este expediente.");

        patientLookup.findPatient(patientId)
                .orElseThrow(() -> new IllegalArgumentException("El paciente no existe."));

        String token = generateSecureToken();

        TemporaryRecordShare share = TemporaryRecordShare.builder()
                .id(UUID.randomUUID())
                .clinicId(clinicId)
                .patientId(patientId)
                .email(email)
                .tokenHash(hashToken(token))
                .createdByUserId(requestingUserId)
                .expiresAt(LocalDateTime.now().plusDays(normalizeDaysValid(daysValid)))
                .createdAt(LocalDateTime.now())
                .build();

        TemporaryRecordShare saved = repository.save(share);
        saved.setPlaintextToken(token);
        return saved;
    }

    @Override
    public SharedRecordSummary getSharedRecord(String token, String ipAddress, String userAgent) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("El enlace de consulta compartida no existe o es invalido.");
        }

        TemporaryRecordShare share = repository.findByTokenHash(hashToken(token))
                .orElseThrow(() -> new IllegalArgumentException("El enlace de consulta compartida no existe o es invalido."));

        if (share.isRevoked()) {
            throw new IllegalArgumentException("El enlace de consulta compartida fue revocado.");
        }
        if (share.isExpired()) {
            throw new IllegalArgumentException("El enlace de consulta compartida ha expirado.");
        }

        PatientLookupPort.PatientDetails patient = patientLookup.findPatient(share.getPatientId())
                .orElseThrow(() -> new IllegalArgumentException("El paciente asociado al expediente ya no existe."));

        String clinicName = clinicLookup.findClinicName(share.getClinicId())
                .orElse("Clinica Medica");

        List<ClinicalNote> notes = noteRepository.findByPatientIdAndClinicIdOrderByCreatedAtDesc(
                share.getPatientId(), share.getClinicId());

        share.registerAccess(LocalDateTime.now());
        repository.save(share);
        recordConsultation(share, ipAddress, userAgent);

        return new SharedRecordSummary(
                patient.id(),
                patient.fullName(),
                patient.curp(),
                patient.phone(),
                patient.email(),
                clinicName,
                notes
        );
    }

    @Override
    public List<ShareLinkView> listActiveShares(UUID clinicId, UUID patientId, UUID requestingUserId) {
        requireReadWrite(requestingUserId, clinicId, patientId, "No tienes permisos para ver los enlaces de este expediente.");
        return repository.findActiveByClinicAndPatient(clinicId, patientId).stream()
                .filter(TemporaryRecordShare::isUsable)
                .map(s -> new ShareLinkView(
                        s.getId(), s.getEmail(), s.getCreatedByUserId(), s.getCreatedAt(),
                        s.getExpiresAt(), s.getLastAccessedAt(), s.getAccessCount()))
                .toList();
    }

    @Override
    public void revokeShare(UUID clinicId, UUID shareId, UUID requestingUserId) {
        TemporaryRecordShare share = repository.findById(shareId)
                .orElseThrow(() -> new IllegalArgumentException("El enlace de consulta compartida no existe."));
        if (!share.getClinicId().equals(clinicId)) {
            throw new IllegalArgumentException("El enlace no pertenece a esta clinica.");
        }
        requireReadWrite(requestingUserId, clinicId, share.getPatientId(),
                "No tienes permisos para revocar enlaces de este expediente.");
        if (share.isRevoked()) {
            return;
        }
        share.setRevokedAt(LocalDateTime.now());
        repository.save(share);
    }

    private void requireReadWrite(UUID requestingUserId, UUID clinicId, UUID patientId, String message) {
        AccessDecision decision = accessAuthorizationPort.resolveAccess(requestingUserId, clinicId, patientId);
        if (decision.level() != AccessLevel.READ_WRITE || decision.viaExternalGrant()) {
            throw new ClinicAccessDeniedException(message);
        }
    }

    private void recordConsultation(TemporaryRecordShare share, String ipAddress, String userAgent) {
        RecordAccessLog log = RecordAccessLog.builder()
                .id(UUID.randomUUID())
                .clinicId(share.getClinicId())
                .patientId(share.getPatientId())
                .userId(null)
                .userName(truncate("enlace-compartido:" + share.getEmail(), 255))
                .resourceType("TEMPORARY_SHARE")
                .resourceId(share.getId())
                .actionType("VIEW")
                .ipAddress(truncate(ipAddress, 64))
                .userAgent(truncate(userAgent, 512))
                .createdAt(LocalDateTime.now())
                .build();
        accessLogOutbox.enqueue(log);
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        TOKEN_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(
                    MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no esta disponible.", e);
        }
    }

    private int normalizeDaysValid(int daysValid) {
        if (daysValid < 1) {
            return 1;
        }
        return Math.min(daysValid, 30);
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
