package com.jclinical.records.domain.service;

import com.jclinical.core.security.ClinicAccessDeniedException;
import com.jclinical.core.security.PatientAccessAuthorizationPort;
import com.jclinical.core.security.PatientAccessAuthorizationPort.AccessDecision;
import com.jclinical.core.security.PatientAccessAuthorizationPort.AccessLevel;
import com.jclinical.records.domain.model.ClinicalNote;
import com.jclinical.records.domain.model.MedicalHistory;
import com.jclinical.records.domain.model.MedicalHistoryTemplate;
import com.jclinical.records.domain.model.Prescription;
import com.jclinical.records.domain.model.RecordAccessLog;
import com.jclinical.records.domain.model.SharedSection;
import com.jclinical.records.domain.model.TemporaryRecordShare;
import com.jclinical.records.domain.model.VitalSigns;
import com.jclinical.records.domain.ports.in.ManageTemporaryShareUseCase.SharedRecordSummary;
import com.jclinical.records.domain.ports.in.ManageTemporaryShareUseCase.SharedStudyContent;
import com.jclinical.records.domain.ports.out.ClinicLookupPort;
import com.jclinical.records.domain.ports.out.ClinicalNoteRepositoryPort;
import com.jclinical.records.domain.ports.out.MedicalHistoryRepositoryPort;
import com.jclinical.records.domain.ports.out.MedicalHistoryTemplateRepositoryPort;
import com.jclinical.records.domain.ports.out.PatientLookupPort;
import com.jclinical.records.domain.ports.out.PrescriptionRepositoryPort;
import com.jclinical.records.domain.ports.out.RecordAccessLogOutboxPort;
import com.jclinical.records.domain.ports.out.SharedStudyLookupPort;
import com.jclinical.records.domain.ports.out.ShareVerificationNotifierPort;
import com.jclinical.records.domain.ports.out.TemporaryRecordShareRepositoryPort;
import com.jclinical.core.security.RecipientVerificationRequiredException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TemporaryRecordShareServiceTest {

    @Mock private TemporaryRecordShareRepositoryPort repository;
    @Mock private ClinicalNoteRepositoryPort noteRepository;
    @Mock private PatientLookupPort patientLookup;
    @Mock private ClinicLookupPort clinicLookup;
    @Mock private PatientAccessAuthorizationPort authorization;
    @Mock private RecordAccessLogOutboxPort accessLogOutbox;
    @Mock private MedicalHistoryRepositoryPort medicalHistoryRepository;
    @Mock private MedicalHistoryTemplateRepositoryPort templateRepository;
    @Mock private PrescriptionRepositoryPort prescriptionRepository;
    @Mock private SharedStudyLookupPort sharedStudyLookup;
    @Mock private ShareVerificationNotifierPort verificationNotifier;

    private TemporaryRecordShareService service;
    private UUID clinicId;
    private UUID patientId;
    private UUID actingUserId;

    @BeforeEach
    void setUp() {
        service = new TemporaryRecordShareService(
                repository, noteRepository, patientLookup, clinicLookup, authorization, accessLogOutbox,
                medicalHistoryRepository, templateRepository, prescriptionRepository, sharedStudyLookup,
                verificationNotifier);
        clinicId = UUID.randomUUID();
        patientId = UUID.randomUUID();
        actingUserId = UUID.randomUUID();
    }

    private void grantReadWrite() {
        when(authorization.resolveAccess(actingUserId, clinicId, patientId))
                .thenReturn(new AccessDecision(AccessLevel.READ_WRITE, false));
    }

    @Test
    void createStoresOnlyHashAndReturnsPlaintextTokenOnce() {
        grantReadWrite();
        when(patientLookup.findPatient(patientId)).thenReturn(Optional.of(patientDetails()));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TemporaryRecordShare created = service.createShareLink(
                clinicId, patientId, "ext@example.com", 7, SharedSection.all(), actingUserId);

        assertThat(created.getPlaintextToken()).isNotBlank();
        ArgumentCaptor<TemporaryRecordShare> saved = ArgumentCaptor.forClass(TemporaryRecordShare.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getTokenHash()).isNotBlank();
        assertThat(saved.getValue().getTokenHash()).isNotEqualTo(created.getPlaintextToken());
        assertThat(saved.getValue().getCreatedByUserId()).isEqualTo(actingUserId);
    }

    @Test
    void createPersistsTheRequestedSections() {
        grantReadWrite();
        when(patientLookup.findPatient(patientId)).thenReturn(Optional.of(patientDetails()));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Set<SharedSection> requested = EnumSet.of(SharedSection.CLINICAL_NOTES, SharedSection.PRESCRIPTIONS);
        service.createShareLink(clinicId, patientId, "ext@example.com", 7, requested, actingUserId);

        ArgumentCaptor<TemporaryRecordShare> saved = ArgumentCaptor.forClass(TemporaryRecordShare.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getSharedSections())
                .containsExactlyInAnyOrder(SharedSection.CLINICAL_NOTES, SharedSection.PRESCRIPTIONS);
    }

    @Test
    void createWithNoSectionsDefaultsToAll() {
        grantReadWrite();
        when(patientLookup.findPatient(patientId)).thenReturn(Optional.of(patientDetails()));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.createShareLink(clinicId, patientId, "ext@example.com", 7, Set.of(), actingUserId);

        ArgumentCaptor<TemporaryRecordShare> saved = ArgumentCaptor.forClass(TemporaryRecordShare.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getSharedSections()).isEqualTo(SharedSection.all());
    }

    @Test
    void createRejectedWhenCallerLacksReadWrite() {
        when(authorization.resolveAccess(actingUserId, clinicId, patientId))
                .thenReturn(new AccessDecision(AccessLevel.READ_ONLY, false));

        assertThatThrownBy(() -> service.createShareLink(
                clinicId, patientId, "ext@example.com", 7, SharedSection.all(), actingUserId))
                .isInstanceOf(ClinicAccessDeniedException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void redeemLooksUpByHashAndLogsTheConsultation() {
        TemporaryRecordShare stored = storedShare(EnumSet.of(SharedSection.CLINICAL_NOTES, SharedSection.VITAL_SIGNS));
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(stored));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(patientLookup.findPatient(patientId)).thenReturn(Optional.of(patientDetails()));
        when(clinicLookup.findClinicName(clinicId)).thenReturn(Optional.of("Clinica Norte"));
        when(noteRepository.findByPatientIdAndClinicIdOrderByCreatedAtDesc(patientId, clinicId)).thenReturn(List.of());

        SharedRecordSummary summary = service.getSharedRecord("some-token", "203.0.113.5", "curl/8");

        assertThat(summary.clinicName()).isEqualTo("Clinica Norte");
        ArgumentCaptor<RecordAccessLog> log = ArgumentCaptor.forClass(RecordAccessLog.class);
        verify(accessLogOutbox).enqueue(log.capture());
        assertThat(log.getValue().getResourceType()).isEqualTo("TEMPORARY_SHARE");
        assertThat(log.getValue().getActionType()).isEqualTo("VIEW");
        assertThat(log.getValue().getUserId()).isNull();
    }

    @Test
    void redeemOnlyLoadsTheSharedSections() {
        TemporaryRecordShare stored = storedShare(EnumSet.of(SharedSection.CLINICAL_NOTES));
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(stored));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(patientLookup.findPatient(patientId)).thenReturn(Optional.of(patientDetails()));
        when(clinicLookup.findClinicName(clinicId)).thenReturn(Optional.of("Clinica Norte"));
        when(noteRepository.findByPatientIdAndClinicIdOrderByCreatedAtDesc(patientId, clinicId))
                .thenReturn(List.of(noteWithVitals()));

        SharedRecordSummary summary = service.getSharedRecord("some-token", "ip", "ua");

        assertThat(summary.clinicalNotes()).hasSize(1);
        assertThat(summary.medicalHistories()).isEmpty();
        assertThat(summary.prescriptions()).isEmpty();
        verify(medicalHistoryRepository, never()).findByPatientIdAndClinicId(any(), any());
        verify(prescriptionRepository, never()).findByClinicIdAndPatientId(any(), any());
    }

    @Test
    void redeemStripsVitalSignsWhenNotShared() {
        TemporaryRecordShare stored = storedShare(EnumSet.of(SharedSection.CLINICAL_NOTES));
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(stored));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(patientLookup.findPatient(patientId)).thenReturn(Optional.of(patientDetails()));
        when(clinicLookup.findClinicName(clinicId)).thenReturn(Optional.of("Clinica Norte"));
        when(noteRepository.findByPatientIdAndClinicIdOrderByCreatedAtDesc(patientId, clinicId))
                .thenReturn(List.of(noteWithVitals()));

        SharedRecordSummary summary = service.getSharedRecord("some-token", "ip", "ua");

        assertThat(summary.clinicalNotes()).singleElement()
                .satisfies(note -> assertThat(note.getVitalSigns()).isNull());
    }

    @Test
    void redeemIncludesHistoryAndPrescriptionsWhenShared() {
        TemporaryRecordShare stored = storedShare(EnumSet.of(SharedSection.MEDICAL_HISTORY, SharedSection.PRESCRIPTIONS));
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(stored));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(patientLookup.findPatient(patientId)).thenReturn(Optional.of(patientDetails()));
        when(clinicLookup.findClinicName(clinicId)).thenReturn(Optional.of("Clinica Norte"));
        UUID templateId = UUID.randomUUID();
        MedicalHistory history = MedicalHistory.builder()
                .id(UUID.randomUUID()).patientId(patientId).clinicId(clinicId).templateId(templateId)
                .answersJson("{\"q1\":\"a1\"}").version(1)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
        when(medicalHistoryRepository.findByPatientIdAndClinicId(patientId, clinicId)).thenReturn(List.of(history));
        when(templateRepository.findByIdAndClinicId(templateId, clinicId)).thenReturn(Optional.of(
                MedicalHistoryTemplate.builder().id(templateId).clinicId(clinicId).name("Antecedentes")
                        .schemaJson("{}").active(true).build()));
        Prescription prescription = Prescription.builder()
                .id(UUID.randomUUID()).clinicId(clinicId).patientId(patientId)
                .notes("Reposo").items(List.of()).createdAt(LocalDateTime.now())
                .build();
        when(prescriptionRepository.findByClinicIdAndPatientId(clinicId, patientId)).thenReturn(List.of(prescription));

        SharedRecordSummary summary = service.getSharedRecord("some-token", "ip", "ua");

        assertThat(summary.medicalHistories()).singleElement()
                .satisfies(view -> assertThat(view.templateName()).isEqualTo("Antecedentes"));
        assertThat(summary.prescriptions()).singleElement()
                .satisfies(view -> assertThat(view.notes()).isEqualTo("Reposo"));
        verify(noteRepository, never()).findByPatientIdAndClinicIdOrderByCreatedAtDesc(any(), any());
    }

    @Test
    void redeemIncludesStudiesWhenShared() {
        TemporaryRecordShare stored = storedShare(EnumSet.of(SharedSection.STUDIES));
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(stored));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(patientLookup.findPatient(patientId)).thenReturn(Optional.of(patientDetails()));
        when(clinicLookup.findClinicName(clinicId)).thenReturn(Optional.of("Clinica Norte"));
        UUID studyId = UUID.randomUUID();
        when(sharedStudyLookup.listStudies(patientId, clinicId, "patient_studies")).thenReturn(List.of(
                new SharedStudyLookupPort.SharedStudy(studyId, "rx.pdf", "application/pdf", 1234L, LocalDateTime.now())));

        SharedRecordSummary summary = service.getSharedRecord("some-token", "ip", "ua");

        assertThat(summary.studies()).singleElement()
                .satisfies(view -> {
                    assertThat(view.id()).isEqualTo(studyId);
                    assertThat(view.filename()).isEqualTo("rx.pdf");
                });
        verify(noteRepository, never()).findByPatientIdAndClinicIdOrderByCreatedAtDesc(any(), any());
    }

    @Test
    void downloadStudyReturnsContentAndLogsIt() {
        TemporaryRecordShare stored = storedShare(EnumSet.of(SharedSection.STUDIES));
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(stored));
        UUID studyId = UUID.randomUUID();
        byte[] bytes = {1, 2, 3};
        when(sharedStudyLookup.readStudy(studyId, patientId, clinicId, "patient_studies"))
                .thenReturn(Optional.of(new SharedStudyLookupPort.SharedStudyContent("rx.pdf", "application/pdf", bytes)));

        SharedStudyContent content = service.getSharedStudyContent("some-token", studyId, "203.0.113.9", "curl/8");

        assertThat(content.bytes()).isEqualTo(bytes);
        assertThat(content.contentType()).isEqualTo("application/pdf");
        ArgumentCaptor<RecordAccessLog> log = ArgumentCaptor.forClass(RecordAccessLog.class);
        verify(accessLogOutbox).enqueue(log.capture());
        assertThat(log.getValue().getResourceType()).isEqualTo("TEMPORARY_SHARE_STUDY");
        assertThat(log.getValue().getResourceId()).isEqualTo(studyId);
        assertThat(log.getValue().getUserId()).isNull();
    }

    @Test
    void downloadStudyRejectedWhenLinkDoesNotShareStudies() {
        TemporaryRecordShare stored = storedShare(EnumSet.of(SharedSection.CLINICAL_NOTES));
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> service.getSharedStudyContent("some-token", UUID.randomUUID(), "ip", "ua"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(sharedStudyLookup, never()).readStudy(any(), any(), any(), any());
        verify(accessLogOutbox, never()).enqueue(any());
    }

    @Test
    void downloadStudyRejectsUnknownAttachment() {
        TemporaryRecordShare stored = storedShare(EnumSet.of(SharedSection.STUDIES));
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(stored));
        when(sharedStudyLookup.readStudy(any(), any(), any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSharedStudyContent("some-token", UUID.randomUUID(), "ip", "ua"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(accessLogOutbox, never()).enqueue(any());
    }

    @Test
    void downloadStudyRejectsRevokedLink() {
        TemporaryRecordShare stored = storedShare(EnumSet.of(SharedSection.STUDIES));
        stored.setRevokedAt(LocalDateTime.now().minusMinutes(1));
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> service.getSharedStudyContent("some-token", UUID.randomUUID(), "ip", "ua"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(sharedStudyLookup, never()).readStudy(any(), any(), any(), any());
    }

    @Test
    void redeemRejectsRevokedLink() {
        TemporaryRecordShare revoked = storedShare(SharedSection.all());
        revoked.setRevokedAt(LocalDateTime.now().minusMinutes(1));
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> service.getSharedRecord("some-token", "ip", "ua"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(accessLogOutbox, never()).enqueue(any());
    }

    @Test
    void redeemRejectsExpiredLink() {
        TemporaryRecordShare expired = storedShare(SharedSection.all());
        expired.setExpiresAt(LocalDateTime.now().minusDays(1));
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.getSharedRecord("some-token", "ip", "ua"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void revokeRequiresReadWriteAndSetsRevokedAt() {
        UUID shareId = UUID.randomUUID();
        TemporaryRecordShare share = storedShare(SharedSection.all());
        share.setId(shareId);
        when(repository.findById(shareId)).thenReturn(Optional.of(share));
        grantReadWrite();
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.revokeShare(clinicId, shareId, actingUserId);

        ArgumentCaptor<TemporaryRecordShare> saved = ArgumentCaptor.forClass(TemporaryRecordShare.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getRevokedAt()).isNotNull();
    }

    @Test
    void redeemRejectsUnverifiedRecipient() {
        TemporaryRecordShare unverified = storedShare(SharedSection.all());
        unverified.setRecipientVerifiedAt(null);
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(unverified));

        assertThatThrownBy(() -> service.getSharedRecord("some-token", "ip", "ua"))
                .isInstanceOf(RecipientVerificationRequiredException.class);
        verify(accessLogOutbox, never()).enqueue(any());
    }

    @Test
    void requestRecipientVerificationGeneratesAndSendsCode() {
        TemporaryRecordShare unverified = storedShare(SharedSection.all());
        unverified.setRecipientVerifiedAt(null);
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(unverified));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.requestRecipientVerification("some-token");

        ArgumentCaptor<TemporaryRecordShare> saved = ArgumentCaptor.forClass(TemporaryRecordShare.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getVerificationCodeHash()).isNotBlank();
        verify(verificationNotifier).sendVerificationCode(eq("ext@example.com"), any(), any());
    }

    @Test
    void requestRecipientVerificationNoOpsWhenAlreadyVerified() {
        TemporaryRecordShare verified = storedShare(SharedSection.all());
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(verified));

        service.requestRecipientVerification("some-token");

        verify(repository, never()).save(any());
        verify(verificationNotifier, never()).sendVerificationCode(any(), any(), any());
    }

    @Test
    void confirmRecipientVerificationSucceedsWithMatchingCode() {
        TemporaryRecordShare unverified = storedShare(SharedSection.all());
        unverified.setRecipientVerifiedAt(null);
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(unverified));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.requestRecipientVerification("some-token");
        ArgumentCaptor<String> sentCode = ArgumentCaptor.forClass(String.class);
        verify(verificationNotifier).sendVerificationCode(any(), sentCode.capture(), any());

        service.confirmRecipientVerification("some-token", sentCode.getValue());

        ArgumentCaptor<TemporaryRecordShare> saved = ArgumentCaptor.forClass(TemporaryRecordShare.class);
        verify(repository, org.mockito.Mockito.atLeastOnce()).save(saved.capture());
        assertThat(saved.getValue().getRecipientVerifiedAt()).isNotNull();
        assertThat(saved.getValue().getVerificationCodeHash()).isNull();
    }

    @Test
    void confirmRecipientVerificationRejectsWrongCodeAndCountsAttempt() {
        TemporaryRecordShare unverified = storedShare(SharedSection.all());
        unverified.setRecipientVerifiedAt(null);
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(unverified));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service.requestRecipientVerification("some-token");

        assertThatThrownBy(() -> service.confirmRecipientVerification("some-token", "WRONGCODE"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(unverified.getVerificationAttempts()).isEqualTo(1);
        assertThat(unverified.getRecipientVerifiedAt()).isNull();
    }

    @Test
    void confirmRecipientVerificationRejectsAfterCodeExpires() {
        TemporaryRecordShare unverified = storedShare(SharedSection.all());
        unverified.setRecipientVerifiedAt(null);
        unverified.setVerificationCodeHash("some-hash");
        unverified.setVerificationCodeExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(unverified));

        assertThatThrownBy(() -> service.confirmRecipientVerification("some-token", "ABC123"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void confirmRecipientVerificationLocksAfterMaxAttempts() {
        TemporaryRecordShare unverified = storedShare(SharedSection.all());
        unverified.setRecipientVerifiedAt(null);
        unverified.setVerificationCodeHash("some-hash");
        unverified.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(5));
        unverified.setVerificationAttempts(5);
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(unverified));

        assertThatThrownBy(() -> service.confirmRecipientVerification("some-token", "ABC123"))
                .isInstanceOf(IllegalStateException.class);
        verify(repository, never()).save(any());
    }

    private TemporaryRecordShare storedShare(Set<SharedSection> sections) {
        return TemporaryRecordShare.builder()
                .id(UUID.randomUUID()).clinicId(clinicId).patientId(patientId).email("ext@example.com")
                .tokenHash("hash")
                .sharedSections(EnumSet.copyOf(sections))
                .expiresAt(LocalDateTime.now().plusDays(3)).createdAt(LocalDateTime.now())
                .recipientVerifiedAt(LocalDateTime.now())
                .build();
    }

    private ClinicalNote noteWithVitals() {
        return ClinicalNote.builder()
                .id(UUID.randomUUID()).patientId(patientId).clinicId(clinicId)
                .subjective("s").objective("o").assessment("a").plan("p")
                .vitalSigns(new VitalSigns(null, null, 70, null, null, null, null, null))
                .createdAt(LocalDateTime.now())
                .build();
    }

    private PatientLookupPort.PatientDetails patientDetails() {
        return new PatientLookupPort.PatientDetails(
                patientId, clinicId, "Juan Perez", "PEPJ900101HDFXXX01", "5550001111", "juan@example.com", "O+");
    }
}
