package com.jclinical.automation.domain.ports.out;

import com.jclinical.automation.domain.model.ChatAccess;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ChatAccessLogPort {

    void record(ChatAccess access);

    /** Lecturas de la clinica; phone y userId opcionales; el mas reciente primero. */
    List<ChatAccess> find(UUID clinicId, String phone, UUID userId, LocalDateTime from, LocalDateTime to, int limit);
}
