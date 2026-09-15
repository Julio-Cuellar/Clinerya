package com.jclinical.records.domain.service;

import com.jclinical.records.domain.model.RecordAccessLog;
import com.jclinical.records.domain.ports.in.ManageRecordAccessLogUseCase.AccessLogCursor;
import com.jclinical.core.security.PatientAccessAuthorizationPort;
import com.jclinical.records.domain.ports.out.PatientValidatorPort;
import com.jclinical.records.domain.ports.out.RecordAccessLogOutboxPort;
import com.jclinical.records.domain.ports.out.RecordAccessLogRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecordAccessLogServiceTest {

    @Mock
    private RecordAccessLogRepositoryPort repository;
    @Mock
    private RecordAccessLogOutboxPort outbox;
    @Mock
    private PatientValidatorPort patientValidator;
    @Mock
    private PatientAccessAuthorizationPort authorization;

    private RecordAccessLogService service;
    private UUID clinicId;
    private UUID patientId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        service = new RecordAccessLogService(repository, outbox, patientValidator, authorization);
        clinicId = UUID.randomUUID();
        patientId = UUID.randomUUID();
        userId = UUID.randomUUID();
        when(patientValidator.existsByIdAndClinicId(patientId, clinicId)).thenReturn(true);
    }

    @Test
    void shouldEnqueueAccessWithoutWritingDirectlyToHistoryTable() {
        service.logAccess(
                clinicId,
                patientId,
                userId,
                "Dra. Prueba",
                "MEDICAL_HISTORY",
                null,
                "READ",
                "127.0.0.1",
                "browser");

        ArgumentCaptor<RecordAccessLog> captor = ArgumentCaptor.forClass(RecordAccessLog.class);
        verify(outbox).enqueue(captor.capture());
        assertThat(captor.getValue().getPatientId()).isEqualTo(patientId);
        assertThat(captor.getValue().getActionType()).isEqualTo("READ");
    }

    @Test
    void shouldReturnOnlyRequestedItemsAndSignalNextPage() {
        when(authorization.resolveAccess(userId, clinicId, patientId))
                .thenReturn(new PatientAccessAuthorizationPort.AccessDecision(
                        PatientAccessAuthorizationPort.AccessLevel.READ_WRITE,
                        false));
        LocalDateTime now = LocalDateTime.now();
        List<RecordAccessLog> fetched = List.of(
                log(now, UUID.randomUUID()),
                log(now.minusSeconds(1), UUID.randomUUID()),
                log(now.minusSeconds(2), UUID.randomUUID()));
        when(repository.findPageByPatientId(patientId, null, null, 3)).thenReturn(fetched);

        var page = service.getAccessLogsByPatient(patientId, clinicId, userId, null, 2);

        assertThat(page.items()).containsExactly(fetched.get(0), fetched.get(1));
        assertThat(page.hasMore()).isTrue();
    }

    @Test
    void shouldForwardCursorToRepository() {
        when(authorization.resolveAccess(userId, clinicId, patientId))
                .thenReturn(new PatientAccessAuthorizationPort.AccessDecision(
                        PatientAccessAuthorizationPort.AccessLevel.READ_ONLY,
                        false));
        AccessLogCursor cursor = new AccessLogCursor(LocalDateTime.now(), UUID.randomUUID());
        when(repository.findPageByPatientId(patientId, cursor.createdAt(), cursor.id(), 51))
                .thenReturn(List.of());

        service.getAccessLogsByPatient(patientId, clinicId, userId, cursor, 50);

        verify(repository).findPageByPatientId(patientId, cursor.createdAt(), cursor.id(), 51);
    }

    @Test
    void shouldRejectUnboundedPageSize() {
        when(authorization.resolveAccess(userId, clinicId, patientId))
                .thenReturn(new PatientAccessAuthorizationPort.AccessDecision(
                        PatientAccessAuthorizationPort.AccessLevel.READ_ONLY,
                        false));

        assertThatThrownBy(() -> service.getAccessLogsByPatient(patientId, clinicId, userId, null, 101))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private RecordAccessLog log(LocalDateTime createdAt, UUID id) {
        return RecordAccessLog.builder()
                .id(id)
                .clinicId(clinicId)
                .patientId(patientId)
                .userId(userId)
                .userName("Dra. Prueba")
                .resourceType("MEDICAL_HISTORY")
                .actionType("READ")
                .createdAt(createdAt)
                .build();
    }
}
