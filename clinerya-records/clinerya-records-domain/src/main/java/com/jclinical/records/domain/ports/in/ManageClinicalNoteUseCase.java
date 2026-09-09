package com.jclinical.records.domain.ports.in;

import com.jclinical.records.domain.model.ClinicalNote;
import com.jclinical.records.domain.model.ClinicalNoteAddendum;
import com.jclinical.records.domain.model.NoteStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ManageClinicalNoteUseCase {

    ClinicalNote createClinicalNote(UUID patientId, UUID clinicId, UUID doctorId, CreateNoteCommand command, UUID requestingUserId);

    ClinicalNote updateClinicalNote(UUID noteId, UUID patientId, UUID clinicId, UpdateNoteCommand command, UUID requestingUserId);

    ClinicalNote signClinicalNote(UUID noteId, UUID patientId, UUID clinicId, UUID requestingUserId, SignNoteCommand command);

    Optional<ClinicalNote> getClinicalNote(UUID noteId, UUID patientId, UUID clinicId, UUID requestingUserId);

    List<ClinicalNote> getClinicalNotesByPatient(UUID patientId, UUID clinicId, UUID requestingUserId);

    /**
     * Agrega un addendum a una nota <b>firmada</b> (NOM-004: la nota original nunca
     * se modifica ni se borra). El addendum se hashea y se registra en la bitácora
     * de firmas.
     */
    ClinicalNoteAddendum addAddendum(UUID noteId, UUID patientId, UUID clinicId,
                                     UUID requestingUserId, AddendumCommand command);

    List<ClinicalNoteAddendum> getAddenda(UUID noteId, UUID patientId, UUID clinicId, UUID requestingUserId);

    record AddendumCommand(
        String content,
        String authorName,
        String ipAddress,
        String userAgent
    ) {}

    record CreateNoteCommand(
        String subjective,
        String objective,
        Double temperature,
        String bloodPressure,
        Integer heartRate,
        Integer respiratoryRate,
        Double weight,
        Double height,
        Integer oxygenSaturation,
        String assessment,
        String plan,
        NoteStatus status
    ) {}

    record UpdateNoteCommand(
        String subjective,
        String objective,
        Double temperature,
        String bloodPressure,
        Integer heartRate,
        Integer respiratoryRate,
        Double weight,
        Double height,
        Integer oxygenSaturation,
        String assessment,
        String plan
    ) {}

    record SignNoteCommand(
        String signerName,
        String ipAddress,
        String userAgent
    ) {}
}
