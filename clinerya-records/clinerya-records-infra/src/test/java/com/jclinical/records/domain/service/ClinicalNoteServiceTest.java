package com.jclinical.records.domain.service;

import com.jclinical.core.security.ClinicAccessDeniedException;
import com.jclinical.records.domain.model.ClinicalNote;
import com.jclinical.records.domain.model.ClinicalNoteAddendum;
import com.jclinical.records.domain.model.ClinicalNoteDiagnosis;
import com.jclinical.records.domain.model.DiagnosisKind;
import com.jclinical.records.domain.model.DocumentSignature;
import com.jclinical.records.domain.model.Icd10Code;
import com.jclinical.records.domain.model.NoteStatus;
import com.jclinical.records.domain.ports.in.ManageClinicalNoteUseCase.AddendumCommand;
import com.jclinical.records.domain.ports.in.ManageClinicalNoteUseCase.DiagnosisEntry;
import com.jclinical.records.domain.ports.in.ManageClinicalNoteUseCase.SignNoteCommand;
import com.jclinical.records.domain.ports.out.ClinicalNoteAddendumRepositoryPort;
import com.jclinical.records.domain.ports.out.ClinicalNoteDiagnosisRepositoryPort;
import com.jclinical.records.domain.ports.out.ClinicalNoteRepositoryPort;
import com.jclinical.records.domain.ports.out.DocumentSignatureRepositoryPort;
import com.jclinical.records.domain.ports.out.Icd10CatalogRepositoryPort;
import com.jclinical.core.security.PatientAccessAuthorizationPort;
import com.jclinical.core.security.PatientAccessAuthorizationPort.AccessDecision;
import com.jclinical.core.security.PatientAccessAuthorizationPort.AccessLevel;
import com.jclinical.records.domain.ports.out.PatientValidatorPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClinicalNoteServiceTest {

    @Mock private ClinicalNoteRepositoryPort noteRepository;
    @Mock private PatientValidatorPort patientValidator;
    @Mock private PatientAccessAuthorizationPort accessAuthorization;
    @Mock private DocumentSignatureRepositoryPort signatureRepository;
    @Mock private ClinicalNoteAddendumRepositoryPort addendumRepository;
    @Mock private ClinicalNoteDiagnosisRepositoryPort diagnosisRepository;
    @Mock private Icd10CatalogRepositoryPort icd10CatalogRepository;

    private ClinicalNoteService service;

    private final UUID clinicId = UUID.randomUUID();
    private final UUID patientId = UUID.randomUUID();
    private final UUID noteId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ClinicalNoteService(noteRepository, patientValidator, accessAuthorization,
                signatureRepository, addendumRepository, diagnosisRepository, icd10CatalogRepository);
    }

    private void grant(AccessLevel level) {
        when(patientValidator.existsByIdAndClinicId(patientId, clinicId)).thenReturn(true);
        when(accessAuthorization.resolveAccess(userId, clinicId, patientId))
                .thenReturn(new AccessDecision(level, false));
    }

    private ClinicalNote note(NoteStatus status) {
        return ClinicalNote.builder()
                .id(noteId).patientId(patientId).clinicId(clinicId).doctorId(UUID.randomUUID())
                .assessment("Dx").plan("Plan").status(status)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    private AddendumCommand cmd(String content) {
        return new AddendumCommand(content, "Dra. Ruiz", "10.0.0.1", "JUnit");
    }

    @Test
    void addAddendumOnSignedNoteHashesAndRegistersSignature() {
        grant(AccessLevel.READ_WRITE);
        when(noteRepository.findByIdAndPatientIdAndClinicId(noteId, patientId, clinicId))
                .thenReturn(Optional.of(note(NoteStatus.SIGNED)));
        when(addendumRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ClinicalNoteAddendum result = service.addAddendum(noteId, patientId, clinicId, userId, cmd("  Corrige dosis  "));

        ArgumentCaptor<ClinicalNoteAddendum> captor = ArgumentCaptor.forClass(ClinicalNoteAddendum.class);
        verify(addendumRepository).save(captor.capture());
        ClinicalNoteAddendum saved = captor.getValue();
        assertThat(saved.getContent()).isEqualTo("Corrige dosis");
        assertThat(saved.getClinicalNoteId()).isEqualTo(noteId);
        assertThat(saved.getCreatedByUserId()).isEqualTo(userId);
        assertThat(saved.getCreatedByUserName()).isEqualTo("Dra. Ruiz");
        assertThat(saved.getIpAddress()).isEqualTo("10.0.0.1");
        assertThat(result.getId()).isEqualTo(saved.getId());

        ArgumentCaptor<DocumentSignature> sig = ArgumentCaptor.forClass(DocumentSignature.class);
        verify(signatureRepository).save(sig.capture());
        assertThat(sig.getValue().getDocumentId()).isEqualTo(saved.getId());
        assertThat(sig.getValue().getDocumentHash()).hasSize(64);
    }

    @Test
    void addAddendumRejectsDraftNote() {
        grant(AccessLevel.READ_WRITE);
        when(noteRepository.findByIdAndPatientIdAndClinicId(noteId, patientId, clinicId))
                .thenReturn(Optional.of(note(NoteStatus.DRAFT)));

        assertThatThrownBy(() -> service.addAddendum(noteId, patientId, clinicId, userId, cmd("algo")))
                .isInstanceOf(IllegalStateException.class);
        verify(addendumRepository, never()).save(any());
        verifyNoInteractions(signatureRepository);
    }

    @Test
    void addAddendumRejectsBlankBody() {
        grant(AccessLevel.READ_WRITE);

        assertThatThrownBy(() -> service.addAddendum(noteId, patientId, clinicId, userId, cmd("   ")))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(addendumRepository, signatureRepository);
    }

    @Test
    void addAddendumDeniedWithoutWriteAccess() {
        grant(AccessLevel.READ_ONLY);

        assertThatThrownBy(() -> service.addAddendum(noteId, patientId, clinicId, userId, cmd("algo")))
                .isInstanceOf(ClinicAccessDeniedException.class);
        verifyNoInteractions(addendumRepository, signatureRepository);
    }

    @Test
    void signClinicalNoteWithoutDiagnosesHashesAndSavesNothingExtra() {
        grant(AccessLevel.READ_WRITE);
        when(noteRepository.findByIdAndPatientIdAndClinicId(noteId, patientId, clinicId))
                .thenReturn(Optional.of(note(NoteStatus.DRAFT)));
        when(noteRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ClinicalNote signed = service.signClinicalNote(noteId, patientId, clinicId, userId,
                new SignNoteCommand("Dra. Ruiz", "10.0.0.1", "JUnit", null));

        assertThat(signed.isSigned()).isTrue();
        assertThat(signed.getDocumentHash()).hasSize(64);
        verifyNoInteractions(diagnosisRepository);
        verify(signatureRepository).save(any());
    }

    @Test
    void signClinicalNoteValidatesAndPersistsDiagnoses() {
        grant(AccessLevel.READ_WRITE);
        when(noteRepository.findByIdAndPatientIdAndClinicId(noteId, patientId, clinicId))
                .thenReturn(Optional.of(note(NoteStatus.DRAFT)));
        when(noteRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(icd10CatalogRepository.findByCode("K04.0"))
                .thenReturn(Optional.of(Icd10Code.builder().code("K04.0").description("Pulpitis").billable(true).build()));
        when(diagnosisRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.signClinicalNote(noteId, patientId, clinicId, userId,
                new SignNoteCommand("Dra. Ruiz", "10.0.0.1", "JUnit",
                        List.of(new DiagnosisEntry("k04.0", DiagnosisKind.PRIMARY))));

        ArgumentCaptor<List<ClinicalNoteDiagnosis>> captor = ArgumentCaptor.forClass(List.class);
        verify(diagnosisRepository).saveAll(captor.capture());
        ClinicalNoteDiagnosis saved = captor.getValue().get(0);
        assertThat(saved.getIcd10Code()).isEqualTo("K04.0");
        assertThat(saved.getKind()).isEqualTo(DiagnosisKind.PRIMARY);
        assertThat(saved.getClinicalNoteId()).isEqualTo(noteId);
    }

    @Test
    void signClinicalNoteRejectsUnknownIcd10Code() {
        grant(AccessLevel.READ_WRITE);
        when(noteRepository.findByIdAndPatientIdAndClinicId(noteId, patientId, clinicId))
                .thenReturn(Optional.of(note(NoteStatus.DRAFT)));
        when(icd10CatalogRepository.findByCode("Z99.9")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.signClinicalNote(noteId, patientId, clinicId, userId,
                new SignNoteCommand("Dra. Ruiz", "10.0.0.1", "JUnit",
                        List.of(new DiagnosisEntry("Z99.9", DiagnosisKind.PRIMARY)))))
                .isInstanceOf(IllegalArgumentException.class);
        verify(noteRepository, never()).save(any());
        verifyNoInteractions(diagnosisRepository);
    }

    @Test
    void signClinicalNoteRejectsNonBillableIcd10Code() {
        grant(AccessLevel.READ_WRITE);
        when(noteRepository.findByIdAndPatientIdAndClinicId(noteId, patientId, clinicId))
                .thenReturn(Optional.of(note(NoteStatus.DRAFT)));
        when(icd10CatalogRepository.findByCode("A00"))
                .thenReturn(Optional.of(Icd10Code.builder().code("A00").description("Colera").billable(false).build()));

        assertThatThrownBy(() -> service.signClinicalNote(noteId, patientId, clinicId, userId,
                new SignNoteCommand("Dra. Ruiz", "10.0.0.1", "JUnit",
                        List.of(new DiagnosisEntry("A00", DiagnosisKind.PRIMARY)))))
                .isInstanceOf(IllegalArgumentException.class);
        verify(noteRepository, never()).save(any());
        verifyNoInteractions(diagnosisRepository);
    }

    @Test
    void signClinicalNoteRejectsMoreThanOnePrimaryDiagnosis() {
        grant(AccessLevel.READ_WRITE);
        when(noteRepository.findByIdAndPatientIdAndClinicId(noteId, patientId, clinicId))
                .thenReturn(Optional.of(note(NoteStatus.DRAFT)));

        assertThatThrownBy(() -> service.signClinicalNote(noteId, patientId, clinicId, userId,
                new SignNoteCommand("Dra. Ruiz", "10.0.0.1", "JUnit",
                        List.of(new DiagnosisEntry("K04.0", DiagnosisKind.PRIMARY),
                                new DiagnosisEntry("K02.9", DiagnosisKind.PRIMARY)))))
                .isInstanceOf(IllegalArgumentException.class);
        verify(noteRepository, never()).save(any());
        verifyNoInteractions(diagnosisRepository, icd10CatalogRepository);
    }

    @Test
    void getDiagnosesReturnsRepositoryListForExistingNote() {
        grant(AccessLevel.READ_ONLY);
        when(noteRepository.findByIdAndPatientIdAndClinicId(noteId, patientId, clinicId))
                .thenReturn(Optional.of(note(NoteStatus.SIGNED)));
        ClinicalNoteDiagnosis one = ClinicalNoteDiagnosis.builder().id(UUID.randomUUID()).clinicalNoteId(noteId).build();
        when(diagnosisRepository.findByClinicalNoteIdAndClinicId(noteId, clinicId)).thenReturn(List.of(one));

        assertThat(service.getDiagnoses(noteId, patientId, clinicId, userId)).containsExactly(one);
    }

    @Test
    void getAddendaReturnsRepositoryListForExistingNote() {
        grant(AccessLevel.READ_ONLY);
        when(noteRepository.findByIdAndPatientIdAndClinicId(noteId, patientId, clinicId))
                .thenReturn(Optional.of(note(NoteStatus.SIGNED)));
        ClinicalNoteAddendum one = ClinicalNoteAddendum.builder().id(UUID.randomUUID()).clinicalNoteId(noteId).build();
        when(addendumRepository.findByClinicalNoteIdAndClinicIdOrderByCreatedAtAsc(noteId, clinicId)).thenReturn(List.of(one));

        assertThat(service.getAddenda(noteId, patientId, clinicId, userId)).containsExactly(one);
    }
}
