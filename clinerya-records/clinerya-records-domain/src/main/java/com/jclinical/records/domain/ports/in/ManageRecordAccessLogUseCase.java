package com.jclinical.records.domain.ports.in;

import com.jclinical.records.domain.model.RecordAccessLog;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ManageRecordAccessLogUseCase {
    RecordAccessLog logAccess(UUID clinicId, UUID patientId, UUID userId, String userName, String resourceType, UUID resourceId, String actionType, String ipAddress, String userAgent);

    AccessLogPage getAccessLogsByPatient(
            UUID patientId,
            UUID clinicId,
            UUID requestingUserId,
            AccessLogCursor cursor,
            int limit);

    record AccessLogCursor(LocalDateTime createdAt, UUID id) {
        public AccessLogCursor {
            if (createdAt == null || id == null) {
                throw new IllegalArgumentException("El cursor de la bitacora esta incompleto.");
            }
        }
    }

    record AccessLogPage(List<RecordAccessLog> items, boolean hasMore) {
        public AccessLogPage {
            items = List.copyOf(items);
        }
    }
}
