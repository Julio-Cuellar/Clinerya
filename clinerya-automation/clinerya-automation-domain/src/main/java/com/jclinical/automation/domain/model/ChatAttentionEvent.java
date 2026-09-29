package com.jclinical.automation.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

/** Bitacora de atencion humana: quien tomo o regreso un chat y cuando ({@code userId} null: el sistema). */
public record ChatAttentionEvent(UUID id, UUID clinicId, String phone, Action action, UUID userId, LocalDateTime at) {

    public enum Action {
        /** El agente pidio ayuda (lo pidio el paciente, no lo entendio o fallo la IA). */
        REQUESTED_BY_AGENT,
        TAKEN,
        RELEASED,
        /** Nadie lo regreso y paso un dia sin actividad: vuelve solo al agente. */
        AUTO_RELEASED;

        public boolean human() {
            return this == REQUESTED_BY_AGENT || this == TAKEN;
        }
    }
}
