package com.jclinical.records.domain.service;

import com.jclinical.core.security.ClinicAccessDeniedException;
import com.jclinical.core.security.PatientAccessAuthorizationPort;
import com.jclinical.core.security.PatientAccessAuthorizationPort.AccessDecision;
import com.jclinical.core.security.PatientAccessAuthorizationPort.AccessLevel;
import com.jclinical.records.domain.model.RecordAccessLog;
import com.jclinical.records.domain.model.TemporaryRecordShare;
import com.jclinical.records.domain.ports.out.ClinicLookupPort;
import com.jclinical.records.domain.ports.out.ClinicalNoteRepositoryPort;
import com.jclinical.records.domain.ports.out.PatientLookupPort;
import com.jclinical.records.domain.ports.out.RecordAccessLogOutboxPort;
import com.jclinical.records.domain.ports.out.TemporaryRecordShareRepositoryPort;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TemporaryRecordShareServiceTest {

    @Mock private TemporaryRecordShareRepositoryPort repository;
    @Mock private ClinicalNoteRepositoryPort noteRepository;
    @Mock private PatientLookupPort patientLookup;
    @Mock private ClinicLookupPort clinicLookup;
    @Mock private PatientAccessAuthorizationPort authorization;
    @Mock private RecordAccessLogOutboxPort accessLogOutbox;

    private TemporaryRecordShareService service;
    private UUID clinicId;
    private UUID patientId;
    private UUID actingUserId;

    @BeforeEach
    void setUp() {
        service = new TemporaryRecordShareService(
                repository, noteRepository, patientLookup, clinicLookup, authorization, accessLogOutbox);
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

        TemporaryRecordShare created = service.createShareLink(clinicId, patientId, "ext@example.com", 7, actingUserId);

        assertThat(created.getPlaintextToken()).isNotBlank();
        ArgumentCaptor<TemporaryRecordShare> saved = ArgumentCaptor.forClass(TemporaryRecordShare.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getTokenHash()).isNotBlank();
        assertThat(saved.getValue().getTokenHash()).isNotEqualTo(created.getPlaintextToken());
        assertThat(saved.getValue().getCreatedByUserId()).isEqualTo(actingUserId);
    }

    @Test
    void createRejectedWhenCallerLacksReadWrite() {
        when(authorization.resolveAccess(actingUserId, clinicId, patientId))
                .thenReturn(new AccessDecision(AccessLevel.READ_ONLY, false));

        assertThatThrownBy(() -> service.createShareLink(clinicId, patientId, "ext@example.com", 7, actingUserId))
                .isInstanceOf(ClinicAccessDeniedException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void redeemLooksUpByHashAndLogsTheConsultation() {
        grantReadWrite();
        when(patientLookup.findPatient(patientId)).thenReturn(Optional.of(patientDetails()));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        TemporaryRecordShare created = service.createShareLink(clinicId, patientId, "ext@example.com", 7, actingUserId);
        String token = created.getPlaintextToken();

        TemporaryRecordShare stored = TemporaryRecordShare.builder()
                .id(UUID.randomUUID()).clinicId(clinicId).patientId(patientId).email("ext@example.com")
                .tokenHash(created.getTokenHash())
                .expiresAt(LocalDateTime.now().plusDays(3)).createdAt(LocalDateTime.now())
                .build();
        when(repository.findByTokenHash(created.getTokenHash())).thenReturn(Optional.of(stored));
        when(clinicLookup.findClinicName(clinicId)).thenReturn(Optional.of("Clinica Norte"));
        when(noteRepository.findByPatientIdAndClinicIdOrderByCreatedAtDesc(patientId, clinicId)).thenReturn(List.of());

        var summary = service.getSharedRecord(token, "203.0.113.5", "curl/8");

        assertThat(summary.clinicName()).isEqualTo("Clinica Norte");
        ArgumentCaptor<RecordAccessLog> log = ArgumentCaptor.forClass(RecordAccessLog.class);
        verify(accessLogOutbox).enqueue(log.capture());
        assertThat(log.getValue().getResourceType()).isEqualTo("TEMPORARY_SHARE");
        assertThat(log.getValue().getActionType()).isEqualTo("VIEW");
        assertThat(log.getValue().getUserId()).isNull();
    }

    @Test
    void redeemRejectsRevokedLink() {
        TemporaryRecordShare revoked = TemporaryRecordShare.builder()
                .id(UUID.randomUUID()).clinicId(clinicId).patientId(patientId).email("ext@example.com")
                .tokenHash("whatever")
                .expiresAt(LocalDateTime.now().plusDays(3)).createdAt(LocalDateTime.now())
                .revokedAt(LocalDateTime.now().minusMinutes(1))
                .build();
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> service.getSharedRecord("some-token", "ip", "ua"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(accessLogOutbox, never()).enqueue(any());
    }

    @Test
    void redeemRejectsExpiredLink() {
        TemporaryRecordShare expired = TemporaryRecordShare.builder()
                .id(UUID.randomUUID()).clinicId(clinicId).patientId(patientId).email("ext@example.com")
                .tokenHash("whatever")
                .expiresAt(LocalDateTime.now().minusDays(1)).createdAt(LocalDateTime.now().minusDays(8))
                .build();
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.getSharedRecord("some-token", "ip", "ua"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void revokeRequiresReadWriteAndSetsRevokedAt() {
        UUID shareId = UUID.randomUUID();
        TemporaryRecordShare share = TemporaryRecordShare.builder()
                .id(shareId).clinicId(clinicId).patientId(patientId).email("ext@example.com")
                .tokenHash("h").expiresAt(LocalDateTime.now().plusDays(3)).createdAt(LocalDateTime.now())
                .build();
        when(repository.findById(shareId)).thenReturn(Optional.of(share));
        grantReadWrite();
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.revokeShare(clinicId, shareId, actingUserId);

        ArgumentCaptor<TemporaryRecordShare> saved = ArgumentCaptor.forClass(TemporaryRecordShare.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getRevokedAt()).isNotNull();
    }

    private PatientLookupPort.PatientDetails patientDetails() {
        return new PatientLookupPort.PatientDetails(
                patientId, clinicId, "Juan Perez", "PEPJ900101HDFXXX01", "5550001111", "juan@example.com", "O+");
    }
}
