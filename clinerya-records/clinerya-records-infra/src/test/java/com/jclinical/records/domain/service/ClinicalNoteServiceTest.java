package com.jclinical.records.domain.service;

import com.jclinical.core.security.ClinicAccessDeniedException;
import com.jclinical.records.domain.model.ClinicalNote;
import com.jclinical.records.domain.model.ClinicalNoteAddendum;
import com.jclinical.records.domain.model.DocumentSignature;
import com.jclinical.records.domain.model.NoteStatus;
import com.jclinical.records.domain.ports.in.ManageClinicalNoteUseCase.AddendumCommand;
import com.jclinical.records.domain.ports.out.ClinicalNoteAddendumRepositoryPort;
import com.jclinical.records.domain.ports.out.ClinicalNoteRepositoryPort;
import com.jclinical.records.domain.ports.out.DocumentSignatureRepositoryPort;
import com.jclinical.records.domain.ports.out.PatientAccessAuthorizationPort;
import com.jclinical.records.domain.ports.out.PatientAccessAuthorizationPort.AccessDecision;
import com.jclinical.records.domain.ports.out.PatientAccessAuthorizationPort.AccessLevel;
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

    private ClinicalNoteService service;

    private final UUID clinicId = UUID.randomUUID();
    private final UUID patientId = UUID.randomUUID();
    private final UUID noteId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ClinicalNoteService(noteRepository, patientValidator, accessAuthorization,
                signatureRepository, addendumRepository);
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
    void getAddendaReturnsRepositoryListForExistingNote() {
        grant(AccessLevel.READ_ONLY);
        when(noteRepository.findByIdAndPatientIdAndClinicId(noteId, patientId, clinicId))
                .thenReturn(Optional.of(note(NoteStatus.SIGNED)));
        ClinicalNoteAddendum one = ClinicalNoteAddendum.builder().id(UUID.randomUUID()).clinicalNoteId(noteId).build();
        when(addendumRepository.findByClinicalNoteIdAndClinicIdOrderByCreatedAtAsc(noteId, clinicId)).thenReturn(List.of(one));

        assertThat(service.getAddenda(noteId, patientId, clinicId, userId)).containsExactly(one);
    }
}
