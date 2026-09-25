package com.jclinical.automation.infra.adapters.out.realtime;

import java.time.LocalDateTime;
import java.util.UUID;

/** Aviso pendiente de entregar por STOMP en {@code destination}. */
public record RealtimePush(String destination, Object payload) {

    /** Un chat tuvo actividad; el contenido se lee por el endpoint auditado. */
    public record ChatActivity(String phone, LocalDateTime at) {}

    /** Cambio en la bandeja del medico. */
    public record InboxChange(UUID requestId, String kind) {}
}
