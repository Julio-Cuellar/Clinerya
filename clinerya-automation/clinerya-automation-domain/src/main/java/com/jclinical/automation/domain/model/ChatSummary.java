package com.jclinical.automation.domain.model;

import java.time.LocalDateTime;
import java.util.List;

/** Un hilo en la lista de chats. No lleva contenido: leer el contenido queda auditado. */
public record ChatSummary(String phone, List<String> patientNames, LocalDateTime lastMessageAt, int messageCount) {

    public ChatSummary {
        patientNames = patientNames == null ? List.of() : List.copyOf(patientNames);
    }
}
