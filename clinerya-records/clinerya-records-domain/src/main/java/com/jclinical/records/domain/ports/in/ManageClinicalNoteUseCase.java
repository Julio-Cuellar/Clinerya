package com.jclinical.records.domain.ports.in;

import com.jclinical.records.domain.model.ClinicalNote;
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
