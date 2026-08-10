package com.jclinical.records.infra.adapters.out.persistence;

import com.jclinical.records.domain.model.RecordAccessLog;
import com.jclinical.records.domain.ports.out.RecordAccessLogRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SqlRecordAccessLogRepository implements RecordAccessLogRepositoryPort {

    private final SpringDataRecordAccessLogRepository springRepository;
    private final RecordAccessLogMapper mapper;

    @Override
    public RecordAccessLog save(RecordAccessLog log) {
        RecordAccessLogEntity entity = mapper.toEntity(log);
        RecordAccessLogEntity saved = springRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<RecordAccessLog> findPageByPatientId(
            UUID patientId,
            LocalDateTime cursorCreatedAt,
            UUID cursorId,
            int pageSize) {
        List<RecordAccessLogEntity> entities = cursorCreatedAt == null
                ? springRepository.findByPatientIdOrderByCreatedAtDescIdDesc(
                        patientId,
                        PageRequest.of(0, pageSize))
                : springRepository.findPageAfterCursor(patientId, cursorCreatedAt, cursorId, pageSize);
        return entities.stream()
                .map(mapper::toDomain)
                .toList();
    }
}
