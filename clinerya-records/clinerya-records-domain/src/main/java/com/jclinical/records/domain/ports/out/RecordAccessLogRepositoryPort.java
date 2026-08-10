package com.jclinical.records.domain.ports.out;

import com.jclinical.records.domain.model.RecordAccessLog;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface RecordAccessLogRepositoryPort {
    RecordAccessLog save(RecordAccessLog log);
    List<RecordAccessLog> findPageByPatientId(
            UUID patientId,
            LocalDateTime cursorCreatedAt,
            UUID cursorId,
            int pageSize);
}
