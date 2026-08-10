package com.jclinical.records.domain.service;

import com.jclinical.core.security.ClinicAccessDeniedException;
import com.jclinical.records.domain.model.ClinicalNote;
import com.jclinical.records.domain.model.TemporaryRecordShare;
import com.jclinical.records.domain.ports.in.ManageTemporaryShareUseCase;
import com.jclinical.records.domain.ports.out.ClinicalNoteRepositoryPort;
import com.jclinical.records.domain.ports.out.ClinicLookupPort;
import com.jclinical.records.domain.ports.out.PatientAccessAuthorizationPort;
import com.jclinical.records.domain.ports.out.PatientAccessAuthorizationPort.AccessDecision;
import com.jclinical.records.domain.ports.out.PatientAccessAuthorizationPort.AccessLevel;
import com.jclinical.records.domain.ports.out.PatientLookupPort;
import com.jclinical.records.domain.ports.out.TemporaryRecordShareRepositoryPort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class TemporaryRecordShareService implements ManageTemporaryShareUseCase {

    private final TemporaryRecordShareRepositoryPort repository;
    private final ClinicalNoteRepositoryPort noteRepository;
    private final PatientLookupPort patientLookup;
    private final ClinicLookupPort clinicLookup;
    private final PatientAccessAuthorizationPort accessAuthorizationPort;

    public TemporaryRecordShareService(TemporaryRecordShareRepositoryPort repository,
                                       ClinicalNoteRepositoryPort noteRepository,
                                       PatientLookupPort patientLookup,
                                       ClinicLookupPort clinicLookup,
                                       PatientAccessAuthorizationPort accessAuthorizationPort) {
        this.repository = repository;
        this.noteRepository = noteRepository;
        this.patientLookup = patientLookup;
        this.clinicLookup = clinicLookup;
        this.accessAuthorizationPort = accessAuthorizationPort;
    }

    @Override
    public TemporaryRecordShare createShareLink(UUID clinicId, UUID patientId, String email, int daysValid, UUID requestingUserId) {
        AccessDecision decision = accessAuthorizationPort.resolveAccess(requestingUserId, clinicId, patientId);
        if (decision.level() != AccessLevel.READ_WRITE || decision.viaExternalGrant()) {
            throw new ClinicAccessDeniedException("No tienes permisos para compartir este expediente.");
        }

        patientLookup.findPatient(patientId)
                .orElseThrow(() -> new IllegalArgumentException("El paciente no existe."));

        String token = generateSecureToken();

        TemporaryRecordShare share = TemporaryRecordShare.builder()
                .id(UUID.randomUUID())
                .clinicId(clinicId)
                .patientId(patientId)
                .email(email)
                .token(token)
                .expiresAt(LocalDateTime.now().plusDays(normalizeDaysValid(daysValid)))
                .createdAt(LocalDateTime.now())
                .build();

        return repository.save(share);
    }

    @Override
    public SharedRecordSummary getSharedRecord(String token) {
        TemporaryRecordShare share = repository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("El enlace de consulta compartida no existe o es inválido."));

        if (share.isExpired()) {
            throw new IllegalArgumentException("El enlace de consulta compartida ha expirado.");
        }

        PatientLookupPort.PatientDetails patient = patientLookup.findPatient(share.getPatientId())
                .orElseThrow(() -> new IllegalArgumentException("El paciente asociado al expediente ya no existe."));

        String clinicName = clinicLookup.findClinicName(share.getClinicId())
                .orElse("Clínica Médica");

        // Ordenamos las notas por fecha de creación descendente para mejor lectura.
        List<ClinicalNote> notes = noteRepository.findByPatientIdAndClinicIdOrderByCreatedAtDesc(share.getPatientId(), share.getClinicId());

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

    private String generateSecureToken() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        java.security.SecureRandom random = new java.security.SecureRandom();
        StringBuilder sb = new StringBuilder(32);
        for (int i = 0; i < 32; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private int normalizeDaysValid(int daysValid) {
        if (daysValid < 1) {
            return 1;
        }
        return Math.min(daysValid, 30);
    }
}
