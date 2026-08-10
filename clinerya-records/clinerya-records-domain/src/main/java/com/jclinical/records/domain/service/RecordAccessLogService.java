package com.jclinical.records.domain.service;

import com.jclinical.core.security.ClinicAccessDeniedException;
import com.jclinical.records.domain.model.RecordAccessLog;
import com.jclinical.records.domain.ports.in.ManageRecordAccessLogUseCase;
import com.jclinical.records.domain.ports.out.PatientAccessAuthorizationPort;
import com.jclinical.records.domain.ports.out.PatientAccessAuthorizationPort.AccessDecision;
import com.jclinical.records.domain.ports.out.PatientAccessAuthorizationPort.AccessLevel;
import com.jclinical.records.domain.ports.out.PatientValidatorPort;
import com.jclinical.records.domain.ports.out.RecordAccessLogOutboxPort;
import com.jclinical.records.domain.ports.out.RecordAccessLogRepositoryPort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class RecordAccessLogService implements ManageRecordAccessLogUseCase {

    private final RecordAccessLogRepositoryPort logRepository;
    private final RecordAccessLogOutboxPort outbox;
    private final PatientValidatorPort patientValidator;
    private final PatientAccessAuthorizationPort accessAuthorizationPort;

    public RecordAccessLogService(
            RecordAccessLogRepositoryPort logRepository,
            RecordAccessLogOutboxPort outbox,
            PatientValidatorPort patientValidator,
            PatientAccessAuthorizationPort accessAuthorizationPort) {
        this.logRepository = logRepository;
        this.outbox = outbox;
        this.patientValidator = patientValidator;
        this.accessAuthorizationPort = accessAuthorizationPort;
    }

    @Override
    public RecordAccessLog logAccess(
            UUID clinicId,
            UUID patientId,
            UUID userId,
            String userName,
            String resourceType,
            UUID resourceId,
            String actionType,
            String ipAddress,
            String userAgent) {
        if (!patientValidator.existsByIdAndClinicId(patientId, clinicId)) {
            throw new IllegalArgumentException("El paciente no existe en esta clinica.");
        }
        RecordAccessLog log = RecordAccessLog.builder()
                .id(UUID.randomUUID())
                .clinicId(clinicId)
                .patientId(patientId)
                .userId(userId)
                .userName(truncate(userName, 255))
                .resourceType(truncate(resourceType, 64))
                .resourceId(resourceId)
                .actionType(truncate(actionType, 32))
                .ipAddress(truncate(ipAddress, 64))
                .userAgent(truncate(userAgent, 512))
                .createdAt(LocalDateTime.now())
                .build();
        outbox.enqueue(log);
        return log;
    }

    @Override
    public AccessLogPage getAccessLogsByPatient(
            UUID patientId,
            UUID clinicId,
            UUID requestingUserId,
            AccessLogCursor cursor,
            int limit) {
        if (!patientValidator.existsByIdAndClinicId(patientId, clinicId)) {
            throw new IllegalArgumentException("El paciente no existe en esta clinica.");
        }
        AccessDecision decision = accessAuthorizationPort.resolveAccess(requestingUserId, clinicId, patientId);
        if (decision.level() == AccessLevel.NONE) {
            throw new ClinicAccessDeniedException("No tienes acceso a este expediente.");
        }
        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException("El limite de la bitacora debe estar entre 1 y 100.");
        }

        List<RecordAccessLog> fetched = logRepository.findPageByPatientId(
                patientId,
                cursor == null ? null : cursor.createdAt(),
                cursor == null ? null : cursor.id(),
                limit + 1);
        boolean hasMore = fetched.size() > limit;
        List<RecordAccessLog> items = hasMore ? fetched.subList(0, limit) : fetched;
        return new AccessLogPage(items, hasMore);
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
