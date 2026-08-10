package com.jclinical.records.infra.config;

import com.jclinical.records.domain.model.ClinicalNote;
import com.jclinical.records.domain.ports.in.ManageClinicalNoteUseCase;
import com.jclinical.records.domain.service.ClinicalNoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Primary
@RequiredArgsConstructor
public class TransactionalClinicalNoteUseCase implements ManageClinicalNoteUseCase {

    private final ClinicalNoteService clinicalNoteService;

    @Override
    @Transactional
    public ClinicalNote createClinicalNote(UUID patientId, UUID clinicId, UUID doctorId, CreateNoteCommand command, UUID requestingUserId) {
        return clinicalNoteService.createClinicalNote(patientId, clinicId, doctorId, command, requestingUserId);
    }

    @Override
    @Transactional
    public ClinicalNote updateClinicalNote(UUID noteId, UUID patientId, UUID clinicId, UpdateNoteCommand command, UUID requestingUserId) {
        return clinicalNoteService.updateClinicalNote(noteId, patientId, clinicId, command, requestingUserId);
    }

    @Override
    @Transactional
    public ClinicalNote signClinicalNote(
            UUID noteId,
            UUID patientId,
            UUID clinicId,
            UUID requestingUserId,
            SignNoteCommand command) {
        return clinicalNoteService.signClinicalNote(noteId, patientId, clinicId, requestingUserId, command);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ClinicalNote> getClinicalNote(UUID noteId, UUID patientId, UUID clinicId, UUID requestingUserId) {
        return clinicalNoteService.getClinicalNote(noteId, patientId, clinicId, requestingUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClinicalNote> getClinicalNotesByPatient(UUID patientId, UUID clinicId, UUID requestingUserId) {
        return clinicalNoteService.getClinicalNotesByPatient(patientId, clinicId, requestingUserId);
    }
}
