package com.jclinical.records.domain.service;

import com.jclinical.core.security.ClinicAccessDeniedException;
import com.jclinical.records.domain.model.ClinicalNote;
import com.jclinical.records.domain.model.ClinicalNoteAddendum;
import com.jclinical.records.domain.model.DocumentSignature;
import com.jclinical.records.domain.model.DocumentSignatureStatus;
import com.jclinical.records.domain.model.NoteStatus;
import com.jclinical.records.domain.model.SignatureDocumentType;
import com.jclinical.records.domain.model.SignerType;
import com.jclinical.records.domain.model.VitalSigns;
import com.jclinical.records.domain.ports.in.ManageClinicalNoteUseCase;
import com.jclinical.records.domain.ports.out.ClinicalNoteAddendumRepositoryPort;
import com.jclinical.records.domain.ports.out.ClinicalNoteRepositoryPort;
import com.jclinical.records.domain.ports.out.DocumentSignatureRepositoryPort;
import com.jclinical.core.security.PatientAccessAuthorizationPort;
import com.jclinical.core.security.PatientAccessAuthorizationPort.AccessDecision;
import com.jclinical.core.security.PatientAccessAuthorizationPort.AccessLevel;
import com.jclinical.records.domain.ports.out.PatientValidatorPort;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ClinicalNoteService implements ManageClinicalNoteUseCase {

    private final ClinicalNoteRepositoryPort noteRepository;
    private final PatientValidatorPort patientValidator;
    private final PatientAccessAuthorizationPort accessAuthorizationPort;
    private final DocumentSignatureRepositoryPort signatureRepository;
    private final ClinicalNoteAddendumRepositoryPort addendumRepository;

    public ClinicalNoteService(ClinicalNoteRepositoryPort noteRepository,
                               PatientValidatorPort patientValidator,
                               PatientAccessAuthorizationPort accessAuthorizationPort,
                               DocumentSignatureRepositoryPort signatureRepository,
                               ClinicalNoteAddendumRepositoryPort addendumRepository) {
        this.noteRepository = noteRepository;
        this.patientValidator = patientValidator;
        this.accessAuthorizationPort = accessAuthorizationPort;
        this.signatureRepository = signatureRepository;
        this.addendumRepository = addendumRepository;
    }

    @Override
    public ClinicalNote createClinicalNote(UUID patientId, UUID clinicId, UUID doctorId, CreateNoteCommand command, UUID requestingUserId) {
        AccessDecision decision = authorize(requestingUserId, patientId, clinicId, true);

        VitalSigns vitalSigns = VitalSigns.create(
                command.temperature(),
                command.bloodPressure(),
                command.heartRate(),
                command.respiratoryRate(),
                command.weight(),
                command.height(),
                command.oxygenSaturation()
        );

        ClinicalNote note = ClinicalNote.builder()
                .id(UUID.randomUUID())
                .patientId(patientId)
                .clinicId(clinicId)
                .doctorId(doctorId)
                .subjective(command.subjective())
                .objective(command.objective())
                .vitalSigns(vitalSigns)
                .assessment(command.assessment())
                .plan(command.plan())
                .status(command.status() != null ? command.status() : NoteStatus.DRAFT)
                .authoredByExternalUserId(decision.viaExternalGrant() ? requestingUserId : null)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return noteRepository.save(note);
    }

    @Override
    public ClinicalNote updateClinicalNote(UUID noteId, UUID patientId, UUID clinicId, UpdateNoteCommand command, UUID requestingUserId) {
        authorize(requestingUserId, patientId, clinicId, true);

        ClinicalNote note = noteRepository.findByIdAndPatientIdAndClinicId(noteId, patientId, clinicId)
                .orElseThrow(() -> new IllegalArgumentException("La nota clinica no existe para este paciente en esta clinica."));

        VitalSigns vitalSigns = VitalSigns.create(
                command.temperature(),
                command.bloodPressure(),
                command.heartRate(),
                command.respiratoryRate(),
                command.weight(),
                command.height(),
                command.oxygenSaturation()
        );

        note.update(
                command.subjective(),
                command.objective(),
                vitalSigns,
                command.assessment(),
                command.plan()
        );

        return noteRepository.save(note);
    }

    @Override
    public ClinicalNote signClinicalNote(
            UUID noteId,
            UUID patientId,
            UUID clinicId,
            UUID requestingUserId,
            SignNoteCommand command) {
        authorize(requestingUserId, patientId, clinicId, true);

        ClinicalNote note = noteRepository.findByIdAndPatientIdAndClinicId(noteId, patientId, clinicId)
                .orElseThrow(() -> new IllegalArgumentException("La nota clinica no existe para este paciente en esta clinica."));

        LocalDateTime signedAt = LocalDateTime.now();
        String documentHash = calculateClinicalNoteHash(note);
        note.sign(requestingUserId, signedAt, documentHash);

        ClinicalNote saved = noteRepository.save(note);
        signatureRepository.save(DocumentSignature.builder()
                .id(UUID.randomUUID())
                .documentType(SignatureDocumentType.CLINICAL_NOTE)
                .documentId(saved.getId())
                .clinicId(saved.getClinicId())
                .patientId(saved.getPatientId())
                .signerType(SignerType.DOCTOR)
                .signerUserId(requestingUserId)
                .signerName(resolveSignerName(command))
                .documentHash(documentHash)
                .ipAddress(command == null ? null : command.ipAddress())
                .userAgent(command == null ? null : command.userAgent())
                .status(DocumentSignatureStatus.ACTIVE)
                .signedAt(signedAt)
                .createdAt(signedAt)
                .build());

        return saved;
    }

    @Override
    public Optional<ClinicalNote> getClinicalNote(UUID noteId, UUID patientId, UUID clinicId, UUID requestingUserId) {
        authorize(requestingUserId, patientId, clinicId, false);
        return noteRepository.findByIdAndPatientIdAndClinicId(noteId, patientId, clinicId);
    }

    @Override
    public List<ClinicalNote> getClinicalNotesByPatient(UUID patientId, UUID clinicId, UUID requestingUserId) {
        authorize(requestingUserId, patientId, clinicId, false);
        return noteRepository.findByPatientIdAndClinicIdOrderByCreatedAtDesc(patientId, clinicId);
    }

    @Override
    public ClinicalNoteAddendum addAddendum(UUID noteId, UUID patientId, UUID clinicId,
                                            UUID requestingUserId, AddendumCommand command) {
        authorize(requestingUserId, patientId, clinicId, true);

        if (command == null || command.content() == null || command.content().isBlank()) {
            throw new IllegalArgumentException("El addendum no puede estar vacio.");
        }

        ClinicalNote note = noteRepository.findByIdAndPatientIdAndClinicId(noteId, patientId, clinicId)
                .orElseThrow(() -> new IllegalArgumentException("La nota clinica no existe para este paciente en esta clinica."));
        if (!note.isSigned()) {
            throw new IllegalStateException("Solo se puede agregar un addendum a una nota clinica firmada.");
        }

        LocalDateTime now = LocalDateTime.now();
        String content = command.content().trim();
        String author = command.authorName() == null || command.authorName().isBlank()
                ? "Medico" : command.authorName().trim();
        UUID addendumId = UUID.randomUUID();
        String documentHash = sha256(String.join("\n",
                "CLINICAL_NOTE_ADDENDUM",
                value(addendumId),
                value(note.getId()),
                value(patientId),
                value(clinicId),
                value(requestingUserId),
                value(content)));

        ClinicalNoteAddendum addendum = addendumRepository.save(ClinicalNoteAddendum.builder()
                .id(addendumId)
                .clinicalNoteId(note.getId())
                .patientId(patientId)
                .clinicId(clinicId)
                .content(content)
                .createdByUserId(requestingUserId)
                .createdByUserName(author)
                .ipAddress(command.ipAddress())
                .userAgent(command.userAgent())
                .createdAt(now)
                .build());

        signatureRepository.save(DocumentSignature.builder()
                .id(UUID.randomUUID())
                .documentType(SignatureDocumentType.CLINICAL_NOTE)
                .documentId(addendum.getId())
                .clinicId(clinicId)
                .patientId(patientId)
                .signerType(SignerType.DOCTOR)
                .signerUserId(requestingUserId)
                .signerName(author)
                .documentHash(documentHash)
                .ipAddress(command.ipAddress())
                .userAgent(command.userAgent())
                .status(DocumentSignatureStatus.ACTIVE)
                .signedAt(now)
                .createdAt(now)
                .build());

        return addendum;
    }

    @Override
    public List<ClinicalNoteAddendum> getAddenda(UUID noteId, UUID patientId, UUID clinicId, UUID requestingUserId) {
        authorize(requestingUserId, patientId, clinicId, false);
        noteRepository.findByIdAndPatientIdAndClinicId(noteId, patientId, clinicId)
                .orElseThrow(() -> new IllegalArgumentException("La nota clinica no existe para este paciente en esta clinica."));
        return addendumRepository.findByClinicalNoteIdAndClinicIdOrderByCreatedAtAsc(noteId, clinicId);
    }

    private AccessDecision authorize(UUID requestingUserId, UUID patientId, UUID clinicId, boolean requireWrite) {
        if (!patientValidator.existsByIdAndClinicId(patientId, clinicId)) {
            throw new IllegalArgumentException("El paciente no existe en esta clinica.");
        }
        AccessDecision decision = accessAuthorizationPort.resolveAccess(requestingUserId, clinicId, patientId);
        if (decision.level() == AccessLevel.NONE || (requireWrite && decision.level() == AccessLevel.READ_ONLY)) {
            throw new ClinicAccessDeniedException("No tienes acceso a este expediente.");
        }
        return decision;
    }

    private String resolveSignerName(SignNoteCommand command) {
        if (command == null || command.signerName() == null || command.signerName().isBlank()) {
            return "Medico";
        }
        return command.signerName().trim();
    }

    private String calculateClinicalNoteHash(ClinicalNote note) {
        String payload = String.join("\n",
                "CLINICAL_NOTE",
                value(note.getId()),
                value(note.getPatientId()),
                value(note.getClinicId()),
                value(note.getDoctorId()),
                value(note.getSubjective()),
                value(note.getObjective()),
                value(note.getAssessment()),
                value(note.getPlan()),
                value(note.getVitalSigns() == null ? null : note.getVitalSigns().temperature()),
                value(note.getVitalSigns() == null ? null : note.getVitalSigns().bloodPressure()),
                value(note.getVitalSigns() == null ? null : note.getVitalSigns().heartRate()),
                value(note.getVitalSigns() == null ? null : note.getVitalSigns().respiratoryRate()),
                value(note.getVitalSigns() == null ? null : note.getVitalSigns().weight()),
                value(note.getVitalSigns() == null ? null : note.getVitalSigns().height()),
                value(note.getVitalSigns() == null ? null : note.getVitalSigns().bmi()),
                value(note.getVitalSigns() == null ? null : note.getVitalSigns().oxygenSaturation()));
        return sha256(payload);
    }

    private String value(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String sha256(String payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("No se pudo calcular el hash del documento firmado.", e);
        }
    }
}
