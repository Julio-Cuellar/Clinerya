package com.jclinical.records.infra.adapters.in.web;

import com.jclinical.records.domain.model.RecordAccessLog;
import com.jclinical.records.domain.ports.in.ManageRecordAccessLogUseCase;
import com.jclinical.records.domain.ports.in.ManageRecordAccessLogUseCase.AccessLogCursor;
import com.jclinical.users.infra.security.CurrentUserResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/patients/{patientId}/access-logs")
@RequiredArgsConstructor
public class RecordAccessLogController {

    private final ManageRecordAccessLogUseCase recordAccessLogUseCase;
    private final CurrentUserResolver currentUserResolver;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<RecordAccessLogPageResponse> getAccessLogs(
            @PathVariable UUID patientId,
            @RequestParam UUID clinicId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "50") int limit) {
        var currentUser = currentUserResolver.getCurrentUser();
        AccessLogCursor decodedCursor = RecordAccessLogCursorCodec.decode(cursor);
        var page = recordAccessLogUseCase.getAccessLogsByPatient(
                patientId,
                clinicId,
                currentUser.getId(),
                decodedCursor,
                limit);
        List<RecordAccessLogResponse> items = page.items().stream()
                .map(this::toResponse)
                .toList();
        String nextCursor = page.hasMore() && !page.items().isEmpty()
                ? cursorFor(page.items().get(page.items().size() - 1))
                : null;
        return ResponseEntity.ok(new RecordAccessLogPageResponse(items, nextCursor, page.hasMore()));
    }

    private String cursorFor(RecordAccessLog log) {
        return RecordAccessLogCursorCodec.encode(new AccessLogCursor(log.getCreatedAt(), log.getId()));
    }

    private RecordAccessLogResponse toResponse(RecordAccessLog log) {
        return new RecordAccessLogResponse(
                log.getId(),
                log.getClinicId(),
                log.getPatientId(),
                log.getUserId(),
                log.getUserName(),
                log.getResourceType(),
                log.getResourceId(),
                log.getActionType(),
                log.getIpAddress(),
                log.getUserAgent(),
                log.getCreatedAt()
        );
    }

    public record RecordAccessLogPageResponse(
            List<RecordAccessLogResponse> items,
            String nextCursor,
            boolean hasMore
    ) {}

    public record RecordAccessLogResponse(
            UUID id,
            UUID clinicId,
            UUID patientId,
            UUID userId,
            String userName,
            String resourceType,
            UUID resourceId,
            String actionType,
            String ipAddress,
            String userAgent,
            java.time.LocalDateTime createdAt
    ) {}
}
