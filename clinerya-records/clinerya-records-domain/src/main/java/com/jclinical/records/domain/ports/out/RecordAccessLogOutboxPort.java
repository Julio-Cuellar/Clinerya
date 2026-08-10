package com.jclinical.records.domain.ports.out;

import com.jclinical.records.domain.model.RecordAccessLog;

public interface RecordAccessLogOutboxPort {
    void enqueue(RecordAccessLog log);
}
