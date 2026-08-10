package com.jclinical.records.infra.adapters.out.persistence;

import com.jclinical.records.domain.model.RecordAccessLog;

public interface RecordAccessLogMapper {
    RecordAccessLogEntity toEntity(RecordAccessLog domain);
    RecordAccessLog toDomain(RecordAccessLogEntity entity);
}
